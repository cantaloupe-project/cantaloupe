package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.async.Task;
import edu.illinois.library.cantaloupe.cache.CacheFacade;

final class PurgeInvalidFromCacheTask extends APITask implements Task {

    @Override
    public void run() throws Exception {
        new CacheFacade().purgeExpired();
    }

}
