/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.config.migrate.messages;

import xyz.kyngs.librelogin.api.Logger;
import xyz.kyngs.librelogin.common.config.ConfigurateHelper;
import xyz.kyngs.librelogin.common.config.migrate.ConfigurationMigrator;

public class FourthMessagesMigrator implements ConfigurationMigrator {
    private static final String LEGACY_PLACEHOLDER = "[2fa_code]";
    private static final String DYNAMIC_PLACEHOLDER = "%2fa%";

    @Override
    public void migrate(ConfigurateHelper helper, Logger logger) {
        migrateMessage(helper, "prompt-login");
        migrateMessage(helper, "sub-title-login");
        migrateMessage(helper, "action-bar-login");
    }

    private void migrateMessage(ConfigurateHelper helper, String key) {
        var message = helper.getString(key);
        if (message != null && message.contains(LEGACY_PLACEHOLDER)) {
            helper.set(key, message.replace(LEGACY_PLACEHOLDER, DYNAMIC_PLACEHOLDER));
        }
    }
}
