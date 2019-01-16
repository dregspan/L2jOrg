package org.l2j.commons.database.model;

import org.l2j.commons.Config;
import org.l2j.commons.database.annotation.Column;
import org.l2j.commons.database.annotation.Table;
import org.springframework.data.annotation.Id;

@Table("accounts")
public class Account extends Entity<String> {

    @Id
    private String login;
    private String password;
    @Column("last_access")
    private Long lastAccess;
    @Column("access_level")
    private Integer accessLevel;
    @Column("last_server")
    private Integer lastServer;
    @Column("last_ip")
    private String lastIP;

    public Account() { }

    public Account(String login, String password, long lastAccess, String lastIP) {
        this.login = login;
        this.password = password;
        this.lastAccess = lastAccess;
        this.lastIP = lastIP;
        this.accessLevel = 0;
        this.lastServer = 1;
    }

    @Override
    public String getId() {
        return login;
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public String getPassword() {
        return password;
    }

    public int getLastServer() {
        return lastServer;
    }

    public boolean isBanned() {
        return accessLevel < 0;
    }

    public void setLastAccess(long lastAccess) {
        this.lastAccess= lastAccess;
    }

    public void setLastIP(String lastIP) {
        this.lastIP = lastIP;
    }

    public void setAccessLevel(int accessLevel) {
        this.accessLevel = accessLevel;
    }

    public boolean isGM() {
        return accessLevel >= Config.GM_MIN;
    }
}
