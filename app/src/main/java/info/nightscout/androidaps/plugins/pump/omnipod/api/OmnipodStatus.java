package info.nightscout.androidaps.plugins.pump.omnipod.api;

import com.google.gson.Gson;

import java.math.BigDecimal;

public class OmnipodStatus {

    public int id_lot;
    public int id_t;
    public long radio_address;

    public int state_active_minutes;
    public int state_alert;
    public int state_basal;
    public int state_bolus;
    public boolean state_faulted;
    public double state_last_updated;
    public int state_progress;

    public double insulin_canceled;
    public double insulin_delivered;
    public double insulin_reservoir;

    public BigDecimal[] var_basal_schedule;
    public int var_utc_offset;

    public int fault_event;
    public int fault_event_rel_time;

    public int last_command_db_id;
    public double last_enacted_temp_basal_start = -1;
    public double last_enacted_temp_basal_duration = -1;
    public double last_enacted_temp_basal_amount = -1;


    public static OmnipodStatus fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, OmnipodStatus.class);
    }

    public String asJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
