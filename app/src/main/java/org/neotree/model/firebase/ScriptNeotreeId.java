package org.neotree.model.firebase;

import android.os.Parcel;
import android.os.Parcelable;

public class ScriptNeotreeId {
    public ScriptNeotreeId(){

    }
    public String getNeotreeIncrementId() {
        return deviceid;
    }

    public void setNeotreeIncrementId(String deviceid) {
        this.deviceid = deviceid;
    }

    public String deviceid;
    public ScriptNeotreeId(String deviceid) {
        this.deviceid = deviceid;

    }
}
