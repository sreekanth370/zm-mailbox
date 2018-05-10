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

package com.zimbra.cs.service.mail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang.RandomStringUtils;

import com.zimbra.common.account.ZAttrProvisioning.PrefPasswordRecoveryAddressStatus;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SetRecoveryEmailRequest;
import com.zimbra.soap.mail.message.SetRecoveryEmailRequest.Op;
import com.zimbra.soap.mail.message.SetRecoveryEmailResponse;

public class SetRecoveryEmail extends DocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        SetRecoveryEmailRequest req = zsc.elementToJaxb(request);
        if (!mbox.getAccount().getBooleanAttr(Provisioning.A_zimbraFeatureResetPasswordEnabled,
            false)) {
            throw ServiceException.PERM_DENIED("password reset feature not enabled.");
        }
        Op op = req.getOp();
        if (op == null) {
            throw ServiceException.INVALID_REQUEST("Invalid operation received.", null);
        }
        switch (op) {
        case sendCode:
            String recoveryEmailAddr = req.getRecoveryEmailAddress();
            if (StringUtil.isNullOrEmpty(recoveryEmailAddr)) {
                throw ServiceException.INVALID_REQUEST("Recovery email address not provided.",
                    null);
            }
            validateEmail(recoveryEmailAddr, account);
            sendCode(recoveryEmailAddr, 1, account, mbox, zsc, octxt);
            break;
        case validateCode:
            String recoveryEmailAddrVerificationCode = req
                .getRecoveryEmailAddressVerificationCode();
            if (StringUtil.isNullOrEmpty(recoveryEmailAddrVerificationCode)) {
                throw ServiceException.INVALID_REQUEST(
                    "Recovery email address verification code not provided.", null);
            }
            validateCode(recoveryEmailAddrVerificationCode, account, mbox, zsc, octxt);
            break;
        case resendCode:
            resendCode(account, mbox, zsc, octxt);
            break;
        case reset:
            reset(mbox, zsc);
            break;
        default:
            throw ServiceException.INVALID_REQUEST("Invalid operation received.", null);
        }
        SetRecoveryEmailResponse resp = new SetRecoveryEmailResponse();
        return zsc.jaxbToElement(resp);
    }

    protected void sendCode(String email, int resendCount, Account account, Mailbox mbox,
        ZimbraSoapContext zsc, OperationContext octxt) throws ServiceException {
        String code = RandomStringUtils.random(8, true, true);
        Account authAccount = getAuthenticatedAccount(zsc);
        long expiry = account.getRecoveryEmailCodeValidity();
        Date now = new Date();
        long expiryTime = now.getTime() + expiry;
        sendRecoveryEmailVerificationCode(authAccount, account, email, code, expiryTime, octxt,
            mbox);
        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddress, email);
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus,
            PrefPasswordRecoveryAddressStatus.pending);
        prefs.put(Provisioning.A_zimbraRecoveryEmailVerificationData,
            email + ":" + code + ":" + expiryTime + ":" + resendCount);
        Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true, zsc.getAuthToken());

    }

    protected void sendRecoveryEmailVerificationCode(Account authAccount, Account ownerAccount,
        String emailIdToVerify, String code, long expiryTime, OperationContext octxt, Mailbox mbox)
        throws ServiceException {
        Locale locale = authAccount.getLocale();
        String ownerAcctDisplayName = ownerAccount.getDisplayName();
        if (ownerAcctDisplayName == null) {
            ownerAcctDisplayName = ownerAccount.getName();
        }
        String subject = L10nUtil.getMessage(MsgKey.verifyRecoveryEmailSubject, locale,
            ownerAcctDisplayName);
        String charset = authAccount.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset,
            MimeConstants.P_CHARSET_UTF8);
        try {

            DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String gmtDate = format.format(expiryTime);
            if (ZimbraLog.account.isDebugEnabled()) {
                ZimbraLog.account.debug(
                    "Expiry of Forwarding address verification code sent to %s is %s",
                    emailIdToVerify, gmtDate);
                ZimbraLog.account.debug("Forwarding address verification code sent to %s is %s",
                    emailIdToVerify, code);
            }
            String mimePartText = L10nUtil.getMessage(MsgKey.verifyRecoveryEmailBodyText, locale, code,
                gmtDate);
            String mimePartHtml = L10nUtil.getMessage(MsgKey.verifyRecoveryEmailBodyHtml, locale, code,
                gmtDate);
            MimeMultipart mmp = AccountUtil.generateMimeMultipart(mimePartText, mimePartHtml, null);
            MimeMessage mm = AccountUtil.generateMimeMessage(authAccount, ownerAccount, subject,
                charset, null, null, emailIdToVerify, mmp);
            mbox.getMailSender().sendMimeMessage(octxt, mbox, false, mm, null, null, null, null,
                false);
        } catch (MessagingException e) {
            ZimbraLog.account
                .warn("Failed to send verification code to email ID: '" + emailIdToVerify + "'", e);
            throw ServiceException
                .FAILURE("Failed to send verification code to email ID: " + emailIdToVerify, e);
        }
    }

    protected void validateCode(String recoveryEmailAddrVerificationCode, Account account,
        Mailbox mbox, ZimbraSoapContext zsc, OperationContext octxt) throws ServiceException {
        String verificationData = account.getRecoveryEmailVerificationData();
        String[] data = verificationData.split(":");
        String code = data[1];
        long expiryTime = Long.parseLong(data[2]);
        Date now = new Date();
        if (expiryTime < now.getTime()) {
            throw ServiceException
                .FAILURE("The recovery email address verification code is expired.", null);

        }
        if (code.equals(recoveryEmailAddrVerificationCode)) {
            HashMap<String, Object> prefs = new HashMap<String, Object>();
            prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus,
                PrefPasswordRecoveryAddressStatus.verified);
            prefs.put(Provisioning.A_zimbraRecoveryEmailVerificationData, null);
            Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true,
                zsc.getAuthToken());

        } else {
            throw ServiceException
                .FAILURE("Verification of recovery email address verification code failed.", null);
        }
    }

    protected void resendCode(Account account, Mailbox mbox, ZimbraSoapContext zsc,
        OperationContext octxt) throws ServiceException {
        String verificationData = account.getRecoveryEmailVerificationData();
        String[] data = verificationData.split(":");
        String email = data[0];
        String code = data[1];
        long expiryTime = Long.parseLong(data[2]);
        int resendCount = Integer.parseInt(data[3]);
        if (resendCount < account.getPasswordRecoveryMaxAttempts()) {
            // check if code is expired
            Date now = new Date();
            if (expiryTime < now.getTime()) {
                // generate new code and send
                sendCode(email, resendCount + 1, account, mbox, zsc, octxt);

            } else {
                // send existing code
                long expiry = account.getRecoveryEmailCodeValidity();
                long newExpiryTime = now.getTime() + expiry;
                Account authAccount = getAuthenticatedAccount(zsc);
                // update resend count
                resendCount = resendCount + 1;
                HashMap<String, Object> prefs = new HashMap<String, Object>();
                prefs.put(Provisioning.A_zimbraRecoveryEmailVerificationData,
                    email + ":" + code + ":" + newExpiryTime + ":" + resendCount);
                Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true,
                    zsc.getAuthToken());
                sendRecoveryEmailVerificationCode(authAccount, account, email, code, newExpiryTime,
                    octxt, mbox);
            }
        } else {
            throw ServiceException.FAILURE("Resend code request has reached maximum limit.", null);
        }
    }

    protected void reset(Mailbox mbox, ZimbraSoapContext zsc) throws ServiceException {
        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddress, null);
        prefs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus, null);
        prefs.put(Provisioning.A_zimbraRecoveryEmailVerificationData, null);
        Provisioning.getInstance().modifyAttrs(mbox.getAccount(), prefs, true, zsc.getAuthToken());

    }

    private static void validateEmail(String email, Account account) throws ServiceException {
        String[] addresses = account.getMailAddress();
        if (Arrays.asList(addresses).contains(email)) {
            throw ServiceException
                .FAILURE("Recovery address should not be same as primary email address.", null);
        }
    }
}