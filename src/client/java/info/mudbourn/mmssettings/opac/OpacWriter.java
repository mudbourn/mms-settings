package info.mudbourn.mmssettings.opac;

import java.lang.reflect.Method;

import info.mudbourn.mmssettings.MmsSettings;
import xaero.pac.client.player.config.api.IPlayerConfigClientStorageAPI;
import xaero.pac.client.player.config.api.v2.IPlayerConfigStringableOptionClientStorageAPI;

final class OpacWriter {
    private static boolean failed;

    private OpacWriter() {
    }

    static boolean hasFailed() {
        return failed;
    }

    static boolean write(IPlayerConfigClientStorageAPI storage, IPlayerConfigStringableOptionClientStorageAPI<?> option, Object value) {
        try {
            option.getClass().getMethod("setCastValue", Object.class).invoke(option, value);

            Object opac = Class.forName("xaero.pac.OpenPartiesAndClaims").getField("INSTANCE").get(null);
            Object clientData = opac.getClass().getMethod("getClientDataInternal").invoke(opac);
            Object synchronizer = clientData.getClass().getMethod("getPlayerConfigClientSynchronizer").invoke(clientData);

            for (Method method : synchronizer.getClass().getMethods()) {
                if (method.getName().equals("syncToServer") && method.getParameterCount() == 2) {
                    method.invoke(synchronizer, storage, option);

                    return true;
                }
            }

            throw new NoSuchMethodException("syncToServer");
        } catch (Throwable error) {
            if (!failed) {
                failed = true;
                MmsSettings.LOGGER.warn("Could not send an OPAC config change, use the OPAC screen instead", error);
            }

            return false;
        }
    }
}
