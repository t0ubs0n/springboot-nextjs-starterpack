package com.toubson.modulith.catalog;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
        allowedDependencies = {"shared::events"}
)
class CatalogModule {}
