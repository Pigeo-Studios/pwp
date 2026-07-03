package com.pwp.core.model;

import java.util.List;

public class CaseDefinition {
    public String caseId;
    public String name;
    public String description;
    public int priceCoins;
    public String iconPath;
    public boolean enabled;
    public String createdAt;
    public List<CaseLootEntry> loot;
}
