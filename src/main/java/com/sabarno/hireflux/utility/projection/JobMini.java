package com.sabarno.hireflux.utility.projection;

import java.util.UUID;

public interface JobMini {
    UUID getId();
    String getTitle();
    String getLocation();
}