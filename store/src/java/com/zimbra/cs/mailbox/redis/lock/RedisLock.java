/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.redis.lock;

import java.util.List;

import org.redisson.client.codec.Codec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.client.protocol.decoder.MultiDecoder;
import org.redisson.command.CommandExecutor;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.UUIDUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.redis.RedisUtils;
import com.zimbra.cs.mailbox.redis.RedissonRetryClient;

public abstract class RedisLock {

    protected String lockName;
    protected RedisLockChannel lockChannel;
    protected String lockChannelName;
    protected String accountId;
    protected RedissonRetryClient client;
    protected String uuid; //unique for each lock instance
    protected String lockId; //unique for each top-level RedisReadWriteLock

    protected static RedisCommand<LockResponse> LOCK_RESPONSE_CMD = new RedisCommand<>("EVAL", new LockResponseConvertor());

    public RedisLock(String accountId, String lockBaseName, String lockId) {
        this.lockChannel = RedisLockChannelManager.getInstance().getLockChannel(accountId);
        this.lockChannelName = lockChannel.getChannelName().getKey();
        this.lockName = RedisUtils.createAccountRoutedKey(lockChannel.getChannelName().getHashTag(), lockBaseName);
        this.accountId = accountId;
        this.client = (RedissonRetryClient) RedissonClientHolder.getInstance().getRedissonClient();
        this.uuid = UUIDUtil.generateUUID();
        this.lockId = lockId;
    }


    public String getAccountId() {
        return accountId;
    }

    public String getUuid() {
        return uuid;
    }

    public String getLockName() {
        return lockName;
    }

    protected String getThreadLockName() {
        long threadId = Thread.currentThread().getId();
        return lockName + ":" + threadId;
    }

    protected long getLeaseTime() {
        return LC.zimbra_mailbox_lock_read_lease_seconds.intValue() *1000;
    }

    private long getTimeout() {
        return LC.zimbra_mailbox_lock_timeout.intValue() * 1000;
    }

    protected String getLastAccessKey() {
        return lockName + ":" + "last_accessed";
    }

    /**
     * Try to acquire the lock in redis
     * @return null if success; otherwise the TTL of the held lock
     */
    protected abstract LockResponse tryAcquire();

    /**
     * Release the lock in redis
     */
    protected abstract Boolean unlockInner();

    public LockResponse lock() throws ServiceException {
        QueuedLockRequest waitingLock = lockChannel.add(this, (context) -> {
            LockResponse response = tryAcquire();
            if (response.success()) {
                if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                    ZimbraLog.mailboxlock.trace("successfully acquired %s: %s", this, context);
                }
            } else {
                if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                    ZimbraLog.mailboxlock.trace("%s received unlock message but it was acquired elsewhere, will try again (%s ms left)", this, context.getRemainingTime());
                }
            }
            return response;
        });
        if (waitingLock.canTryAcquireNow()) {
            LockResponse response = tryAcquire();
            if (response.success()) {
                if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                    ZimbraLog.mailboxlock.trace("successfully acquired %s without waiting", this);
                }
                lockChannel.remove(waitingLock);
                return response;
            }
            long timeout = Math.min(response.getTTL(), getTimeout());
            return lockChannel.waitForUnlock(waitingLock, timeout);
        } else {
            return lockChannel.waitForUnlock(waitingLock, getTimeout());
        }
    }

    protected <T> T execute(String script, Codec codec, RedisCommand<T> commandType, List<Object> keys, Object... values) {
        CommandExecutor executor = client.getCommandExecutor();
        return executor.evalWrite(lockName, codec, commandType, script, keys, values);
    }

    public void unlock() {
        if (ZimbraLog.mailboxlock.isTraceEnabled()) {
            ZimbraLog.mailboxlock.trace("releasing redis lock %s from thread %s", this, getThreadLockName());
        }
        if (unlockInner() == null) {
            ZimbraLog.mailboxlock.warn("%s attempted to release a lock it did not hold!", getThreadLockName());
        }
    }

    protected String getUnlockMsg() {
        return accountId;
    }

    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("uuid", uuid)
                .add("accountId", accountId);
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }

    public static class LockResponse {

        private Long ttl;
        private String lastWriter;
        private boolean firstReadSinceLastWrite;

        public LockResponse(Long ttl) {
            this.ttl = ttl;
        }

        public LockResponse(String lastWriter, boolean firstReadSinceLastWrite) {
            this.lastWriter = lastWriter;
            this.firstReadSinceLastWrite = firstReadSinceLastWrite;
        }

        public boolean success() {
            return ttl == null;
        }

        public Long getTTL() {
            return ttl;
        }

        public String getLastWriter() {
            return lastWriter;
        }

        public boolean isFirstReadSinceLastWrite() {
            return firstReadSinceLastWrite;
        }
    }

    private static class LockResponseConvertor implements MultiDecoder<LockResponse> {

        @Override
        public Decoder<Object> getDecoder(int paramNum, State state) {
            return null;
        }

        @Override
        public LockResponse decode(List<Object> parts, State state) {
            ZimbraLog.mailboxlock.info("LockResponseConvertor received response %s", parts);
            if (parts.get(0).equals(Long.valueOf(1))) {
                //lock success
                String lastWriter = (String) parts.get(1);
                boolean isFirstReadSinceLastWrite = parts.size() == 3 ? (parts.get(2)).equals(Long.valueOf(1)) : true;
                return new LockResponse(lastWriter, isFirstReadSinceLastWrite);
            } else {
                //failed to acquire lock, returning TTL
                Long ttl = (Long) parts.get(1);
                return new LockResponse(ttl);
            }
        }
    }
}
