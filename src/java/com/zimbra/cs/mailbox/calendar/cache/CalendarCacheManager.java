/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.util.Zimbra;

public class CalendarCacheManager {

    // for appointment summary caching (primarily for ZWC)
    protected boolean mSummaryCacheEnabled;
    protected CalSummaryCache mSummaryCache;

    // for CalDAV ctag caching
    protected CalListCache mCalListCache;
    protected CtagInfoCache mCtagCache;
    protected CtagResponseCache mCtagResponseCache;


    private static CalendarCacheManager sInstance = new CalendarCacheManager();

    public static CalendarCacheManager getInstance() { return sInstance; }

    @VisibleForTesting
    public static void setInstance(CalendarCacheManager instance) {sInstance = instance;}

    public CalendarCacheManager() {
        mCtagCache = new CtagInfoCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(mCtagCache);
        Zimbra.getAppContext().getAutowireCapableBeanFactory().initializeBean(mCtagCache, "ctagInfoCache");

        mCalListCache = new MemcachedCalListCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(mCalListCache);
        Zimbra.getAppContext().getAutowireCapableBeanFactory().initializeBean(mCalListCache, "calListCache");

        mCtagResponseCache = new CtagResponseCache();

        int summaryLRUSize = 0;
        mSummaryCacheEnabled = LC.calendar_cache_enabled.booleanValue();
        if (mSummaryCacheEnabled)
            summaryLRUSize = LC.calendar_cache_lru_size.intValue();
        mSummaryCache = new CalSummaryCache(summaryLRUSize);
    }

    public void notifyCommittedChanges(PendingModifications mods, int changeId) {
        if (mSummaryCacheEnabled)
            mSummaryCache.notifyCommittedChanges(mods, changeId);
        new CalListCacheMailboxListener(mCalListCache).notifyCommittedChanges(mods, changeId);
        if (Zimbra.getAppContext().getBean(ZimbraMemcachedClient.class).isConnected()) {
            mCtagCache.notifyCommittedChanges(mods, changeId);
        }
    }

    public void purgeMailbox(Mailbox mbox) throws ServiceException {
        mSummaryCache.purgeMailbox(mbox);
        mCalListCache.remove(mbox);
        if (Zimbra.getAppContext().getBean(ZimbraMemcachedClient.class).isConnected()) {
            mCtagCache.purgeMailbox(mbox);
        }
    }

    CtagInfoCache getCtagCache() { return mCtagCache; }
    public CalSummaryCache getSummaryCache() { return mSummaryCache; }
    public CtagResponseCache getCtagResponseCache() { return mCtagResponseCache; }

    public AccountCtags getCtags(AccountKey key) throws ServiceException {
        CalList calList = mCalListCache.get(key.getAccountId());
        if (calList == null) {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(key.getAccountId());
            if (mbox == null) {
                ZimbraLog.calendar.warn("Invalid account %s during cache lookup", key.getAccountId());
                return null;
            }
            List<Folder> calFolders = mbox.getCalendarFolders(null, SortBy.NONE);
            Set<Integer> idset = new HashSet<Integer>(calFolders.size());
            idset.add(Mailbox.ID_FOLDER_INBOX);  // Inbox is always included for scheduling support.
            for (Folder calFolder : calFolders) {
                idset.add(calFolder.getId());
            }
            calList = new CalList(idset);
            mCalListCache.put(key.getAccountId(), calList);
        }

        Collection<Integer> calendarIds = calList.getCalendars();
        List<CalendarKey> calKeys = new ArrayList<CalendarKey>(calendarIds.size());
        String accountId = key.getAccountId();
        for (int calFolderId : calendarIds) {
            calKeys.add(new CalendarKey(accountId, calFolderId));
        }
        Map<CalendarKey, CtagInfo> ctagsMap = mCtagCache.getMulti(calKeys);
        AccountCtags acctCtags = new AccountCtags(calList, ctagsMap.values());
        return acctCtags;
    }
}
