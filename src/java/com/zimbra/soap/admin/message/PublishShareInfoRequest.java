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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DistributionListSelector;
import com.zimbra.soap.admin.type.ShareInfoSelector;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_PUBLISH_SHARE_INFO_REQUEST)
public class PublishShareInfoRequest {

    @XmlElement(name=AdminConstants.E_DL, required=false)
    private final DistributionListSelector dl;
    @XmlElement(name=AdminConstants.E_SHARE, required=false)
    private final ShareInfoSelector share;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private PublishShareInfoRequest() {
        this((DistributionListSelector) null, (ShareInfoSelector) null);
    }

    public PublishShareInfoRequest(DistributionListSelector dl,
            ShareInfoSelector share) {
        this.dl = dl;
        this.share = share;
    }

    public DistributionListSelector getDL() { return dl; }
    public ShareInfoSelector getShare() { return share; }
}
