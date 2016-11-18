/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Zimbra, Inc.
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

package com.zimbra.common.soap;

import org.dom4j.QName;

public class SmimeConstants {

    public static final String E_SEND_SECURE_MSG_REQUEST = "SendSecureMsgRequest";
    public static final String E_SEND_SECURE_MSG_RESPONSE = "SendSecureMsgResponse";
    public static final String E_GET_SMIME_CERT_INFO_REQUEST = "GetSmimeCertificateInfoRequest";
    public static final String E_GET_SMIME_CERT_INFO_RESPONSE = "GetSmimeCertificateInfoResponse";
    public static final String E_SAVE_SMIME_CERTIFICATE_REQUEST = "SaveSmimeCertificateRequest";
    public static final String E_SAVE_SMIME_CERTIFICATE_RESPONSE = "SaveSmimeCertificateResponse";

    public static final QName SEND_SECURE_MSG_REQUEST = QName.get(E_SEND_SECURE_MSG_REQUEST, MailConstants.NAMESPACE);
    public static final QName SEND_SECURE_MSG_RESPONSE = QName.get(E_SEND_SECURE_MSG_RESPONSE, MailConstants.NAMESPACE);
    public static final QName GET_SMIME_CERT_INFO_REQUEST = QName.get(E_GET_SMIME_CERT_INFO_REQUEST, AccountConstants.NAMESPACE);
    public static final QName GET_SMIME_CERT_INFO_RESPONSE = QName.get(E_GET_SMIME_CERT_INFO_RESPONSE, AccountConstants.NAMESPACE);
    public static final QName SAVE_SMIME_CERTIFICATE_REQUEST = QName.get(E_SAVE_SMIME_CERTIFICATE_REQUEST, AccountConstants.NAMESPACE);
    public static final QName SAVE_SMIME_CERTIFICATE_RESPONSE = QName.get(E_SAVE_SMIME_CERTIFICATE_RESPONSE, AccountConstants.NAMESPACE);

    public static final String A_SIGN = "sign";
    public static final String A_IS_SIGNED = "isSigned";
    public static final String A_ENCRYPT = "encrypt";
    public static final String A_CERT_ID = "certId";
    public static final String A_CERTIFICATE_PASSWORD = "password";
    public static final String A_REPLACE_ID = "replaceId";
    public static final String A_PUB_CERT_ID = "pubCertId";
    public static final String A_PVT_KEY_ID = "pvtKeyId";
    public static final String A_DEFAULT = "default";

    public static final String E_CERTIFICATE = "certificate";
    public static final String E_EMAIL_ADDR = "emailAddress";
    public static final String E_SUBJECT_DN = "issuedTo";
    public static final String E_ISSUER_DN = "issuedBy";
    public static final String E_SERIAL_NO = "serialNo";
    public static final String E_ALGORITHM = "algorithm";
    public static final String E_VALIDITY = "validity";
    public static final String E_SIGNATURE = "signature";
    public static final String E_START_DATE = "startDate";
    public static final String E_END_DATE = "endDate";
    public static final String E_SUBJECT_ALT_NAME = "subjectAltName";
    public static final String E_ISSUER_ALT_NAME = "issuerAltName";
    public static final String E_ERROR_CODE = "errorCode";
    public static final String E_ERROR_DETAIL = "errorDetail";

    //distinguished name
    public static final String E_COUNTRY = "c";
    public static final String E_STATE = "st";
    public static final String E_CITY = "l";
    public static final String E_ORG = "o";
    public static final String E_ORG_UNIT = "ou";
    public static final String E_COMMON_NAME = "cn";

    //alt names
    public static final String E_OTHER_NAME = "otherName";
    public static final String E_RFC822_NAME = "rfc822Name";
    public static final String E_DNS_NAME = "dNSName";
    public static final String E_X400ADDRESS = "x400Address";
    public static final String E_DIRECTORY_NAME = "directoryName";
    public static final String E_EDI_PARTY_NAME ="ediPartyName";
    public static final String E_URI = "uniformResourceIdentifier";
    public static final String E_IP_ADDRESS = "iPAddress";
    public static final String E_REGISTERED_ID = "registeredID";

    public static final String PUB_CERT = "pubCert";
    public static final String PVT_KEY = "pvtKey";
    public static final String ALIAS = "alias";
    public static final String CERT_FOLDER_NAME = "-smimecertificates";
    public static final String SUBJECT_DN_KEY = "it_";
    public static final String ISSUER_DN_KEY = "ib_";
    public static final String SUBJECT_ALT_NAME_KEY = "san_";
    public static final String ISSUER_ALT_NAME_KEY = "ian_";
    public static final String CERT_SUFFIX = ".pem";
    public static final String KEY_SUFFIX = ".key";
    public static final String PUB_CERT_TYPE = "X.509";
    public static final String PRIVATE_KEY_ALGORITHM = "keyAlgorithm";
    public static final String PRIVATE_KEY_FORMAT = "keyFormat";
    public static final String PRIVATE_KEY_INFO = "privateKeyInfo";
    public static final String PUBLIC_CERT_INFO = "pubCertInfo";
    public static final String MIME_TYPE_MULTIPART_SIGNED = "multipart/signed";
    public static final String MIME_TYPE_APPLICATION_PKCS7MIME = "application/pkcs7-mime";
    public static final String MIME_TYPE_APPLICATION_XPKCS7MIME = "application/x-pkcs7-mime";
    public static final String SMIME_TYPE_ENVELOPED_DATA = "enveloped-data";
    public static final String SMIME_CONTENT_TRANSFER_ENCODING_BASE64 = "base64";

    public static final String CMS_VERIFIER_NOT_VALID_AT_SIGNINGTIME_ERROR_MESSAGE = "verifier not valid at signingTime";
    public static final String CMS_SIGNER_DIGEST_MISMATCH_ERROR_MESSAGE = "message-digest attribute value does not match calculated value";
    public static final String CMS_EMAILADDRESS_NOMATCH_ERROR_MESSAGE = "Email address doesn't match";
    public static final String CMS_CLIENT_CERTIFICATE_EXPIRED_ERROR_MESSAGE = "client certificate expired";
    public static final String CMS_CLIENT_CERTIFICATE_NOT_YET_VALID_ERROR_MESSAGE = "client certificate not yet valid";
}
