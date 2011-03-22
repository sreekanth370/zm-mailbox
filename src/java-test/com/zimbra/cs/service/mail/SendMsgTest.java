/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.HSQLDB;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.mail.ToXML.EmailType;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.ZimbraSoapContext;

public class SendMsgTest {

    @BeforeClass
    public static void init() throws Exception {
        Provisioning prov = new MockProvisioning();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        prov.createAccount("test2@zimbra.com", "secret", new HashMap<String, Object>());
        Provisioning.setInstance(prov);

        LC.zimbra_class_database.setDefault(HSQLDB.class.getName());
        DbPool.startup();
        HSQLDB.createDatabase();

        // this MailboxManager does everything except actually send mail
        MailboxManager.setInstance(new MailboxManager() {
            @Override
            protected Mailbox instantiateMailbox(MailboxData data) {
                return new Mailbox(data) {
                    @Override
                    public MailSender getMailSender() {
                        return new MailSender() {
                            @Override
                            protected Collection<Address> sendMessage(Mailbox mbox, MimeMessage mm, Collection<RollbackData> rollbacks)
                            throws SafeMessagingException, IOException {
                                try {
                                    return Arrays.asList(getRecipients(mm));
                                } catch (Exception e) {
                                    return Collections.emptyList();
                                }
                            }
                        };
                    }
                };
            }
        });

        LC.zimbra_class_store.setDefault(MockStoreManager.class.getName());
        StoreManager.getInstance().startup();
    }

    @Before
    public void setUp() throws Exception {
        HSQLDB.clearDatabase();
        MailboxManager.getInstance().clearCache();
    }

    @Test
    public void testDeleteDraft() throws Exception {
        Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // first, add draft message
        ParsedMessage pm = new ParsedMessage(new MimeMessage(Session.getInstance(new Properties())), false);
        int draftId = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT).getId();

        // then send a message referencing the draft
        Element request = new Element.JSONElement(MailConstants.SEND_MSG_REQUEST);
        Element m = request.addElement(MailConstants.E_MSG).addAttribute(MailConstants.A_DRAFT_ID, draftId).addAttribute(MailConstants.E_SUBJECT, "dinner appt");
        m.addUniqueElement(MailConstants.E_MIMEPART).addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain").addAttribute(MailConstants.E_CONTENT, "foo bar");
        m.addElement(MailConstants.E_EMAIL).addAttribute(MailConstants.A_ADDRESS_TYPE, EmailType.TO.toString()).addAttribute(MailConstants.A_ADDRESS, "rcpt@zimbra.com");

        Map<String, Object> context = new HashMap<String, Object>();
        context.put(SoapEngine.ZIMBRA_CONTEXT, new ZimbraSoapContext(AuthProvider.getAuthToken(acct), acct.getId(), SoapProtocol.SoapJS, SoapProtocol.SoapJS));
        new SendMsg().handle(request, context);

        // finally, verify that the draft is gone
        Message draft = null;
        try {
            draft = mbox.getMessageById(null, draftId);
        } catch (NoSuchItemException nsie) {
        }
        Assert.assertNull(draft, "draft message not deleted");
    }
}
