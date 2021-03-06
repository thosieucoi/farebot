/*
 * KMTTransitFactory.java
 *
 * Authors:
 * Bondan Sumbodo <sybond@gmail.com>
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
 *
 */

package com.codebutler.farebot.transit.kmt;

import androidx.annotation.NonNull;
import com.codebutler.farebot.base.util.ByteArray;
import com.codebutler.farebot.card.felica.FelicaBlock;
import com.codebutler.farebot.card.felica.FelicaCard;
import com.codebutler.farebot.card.felica.FelicaService;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import net.kazzz.felica.lib.Util;
import java.util.ArrayList;
import java.util.List;

public class KMTTransitFactory implements TransitFactory<FelicaCard, KMTTransitInfo> {

    //Taken from NXP TagInfo reader data

    //This should be in the FeliCaLib from Klazz
    private static final int SYSTEMCODE_KMT = 0x90b7;

    private static final int FELICA_SERVICE_KMT_ID = 0x300B;
    private static final int FELICA_SERVICE_KMT_BALANCE = 0x1017;
    private static final int FELICA_SERVICE_KMT_HISTORY = 0x200F;

    @Override
    public boolean check(@NonNull FelicaCard card) {
        return (card.getSystem(SYSTEMCODE_KMT) != null);
    }

    @NonNull
    @Override
    public TransitIdentity parseIdentity(@NonNull FelicaCard card) {
        FelicaService serviceID = card.getSystem(SYSTEMCODE_KMT).getService(FELICA_SERVICE_KMT_ID);
        String serialNumber = "-";
        if (serviceID != null) {
            serialNumber = new String(serviceID.getBlocks().get(0).getData().bytes());
        }

        return TransitIdentity.create("Kartu Multi Trip", serialNumber);
    }

    @NonNull
    @Override
    public KMTTransitInfo parseInfo(@NonNull FelicaCard card) {
        FelicaService serviceID = card.getSystem(SYSTEMCODE_KMT).getService(FELICA_SERVICE_KMT_ID);
        ByteArray serialNumber;
        if (serviceID != null) {
            serialNumber = new ByteArray(serviceID.getBlocks().get(0).getData().bytes());
        } else {
            serialNumber = new ByteArray("000000000000000".getBytes());
        }
        FelicaService serviceBalance = card.getSystem(SYSTEMCODE_KMT).getService(FELICA_SERVICE_KMT_BALANCE);
        int currentBalance = 0;
        if (serviceBalance != null) {
            List<FelicaBlock> blocksBalance = serviceBalance.getBlocks();
            FelicaBlock blockBalance = blocksBalance.get(0);
            byte[] dataBalance = blockBalance.getData().bytes();
            currentBalance = Util.toInt(dataBalance[3], dataBalance[2], dataBalance[1], dataBalance[0]);
        }
        FelicaService serviceHistory = card.getSystem(SYSTEMCODE_KMT).getService(FELICA_SERVICE_KMT_HISTORY);
        List<Trip> trips = new ArrayList<>();
        List<FelicaBlock> blocks = serviceHistory.getBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            FelicaBlock block = blocks.get(i);
            if (block.getData().bytes()[0] != 0) {
                KMTTrip trip = KMTTrip.create(block);
                trips.add(trip);
            }
        }
        return KMTTransitInfo.create(trips, serialNumber, currentBalance);
    }

}
