/*
 * Copyright 2018 Broadband Forum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadband_forum.obbaa.netconf.api.client.authentication;

public class LoginKey {

    private String m_username;
    private String m_pubKeyFile;
    private String m_privKeyFile;

    public LoginKey(String userName, String pubKeyFile, String privKeyFile) {
        this.m_username = userName;
        this.m_pubKeyFile = pubKeyFile;
        this.m_privKeyFile = privKeyFile;
    }

    public String getUserName() {
        return m_username;
    }

    public void setUserName(String userName) {
        this.m_username = userName;
    }

    public String getPubKeyFile() {
        return m_pubKeyFile;
    }

    public String getPrivKeyFile() {
        return m_privKeyFile;
    }

}
