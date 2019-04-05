package info.nightscout.androidaps.plugins.pump.omnipod;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import java.util.Objects;

import butterknife.OnClick;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.plugins.common.SubscriberFragment;
import info.nightscout.androidaps.plugins.pump.omnipod.api.OmnipyRestApi;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyCallback;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyResult;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipodUpdateGui;

public class OmnipodFragment extends SubscriberFragment implements View.OnClickListener {

    public OmnipodFragment() {
        // Required empty public constructor
    }

    public static OmnipodFragment newInstance() {
        return new OmnipodFragment();
    }

    private OmnipodPdm _pdm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Intent intent = Objects.requireNonNull(getActivity()).getIntent();
//        if (intent != null) {
//
//            Bundle extras = intent.getExtras();
//            if (extras != null) {
//                String address = extras.getString("omnipy_address");
//                if (address != null) {
//                    //
//                }
//            }
//        }

        OmnipodPlugin op = OmnipodPlugin.getPlugin();
        _pdm = op.getPdm();
    }

    @Subscribe
    public void onStatusEvent(final EventOmnipodUpdateGui ev) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> updateGUI());
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_omnipod, container, false);

        view.findViewById(R.id.omnipy_btn_check_connection).setOnClickListener(this);
        view.findViewById(R.id.omnipy_btn_check_rl).setOnClickListener(this);
        view.findViewById(R.id.omnipy_btn_update_status).setOnClickListener(this);
        view.findViewById(R.id.omnipy_btn_clear_alerts).setOnClickListener(this);
        view.findViewById(R.id.omnipy_btn_deactivate_pod).setOnClickListener(this);
        view.findViewById(R.id.omnipy_btn_shutdown_remote_host).setOnClickListener(this);
        view.findViewById(R.id.omnipy_btn_restart_remote_host).setOnClickListener(this);
        view.findViewById(R.id.omnipy_btn_register_pod).setOnClickListener(this);
        return view;
    }

    private void Toast(CharSequence text, boolean shortMessage)
    {
        Context context = getActivity().getApplicationContext();
        int duration = shortMessage ? Toast.LENGTH_SHORT: Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    private void UpdateStatus(Button b) {
        OmnipyRestApi rest = _pdm.GetRestApi();
        if (rest.isConfigured()) {
            b.setEnabled(false);
            Toast("Requesting status update", true);

            rest.UpdateStatus(result -> {
                if (result.success) {
                    Toast("Status updated", true);
                } else {
                    Toast("Status update failed", true);
                }
                b.setEnabled(true);
                updateGUI();

            });
        }
        else
            DisplayMessage("Need to connect to omnipy in order to perform action");

    }

    private void CheckConnection(Button b) {
        OmnipyRestApi rest = _pdm.GetRestApi();
        if (rest.isConfigured()) {
            b.setEnabled(false);
            rest.CheckAuthentication(result -> {
                if (result.success) {
                    Toast("Connection successful!", true);
                } else {
                    Toast("Connection failed", true);
                }
                b.setEnabled(true);
                updateGUI();
            });
            }
        else
            Toast("Omnipy is not available", true);
    }

    private void DeactivatePod(Button b){
        OmnipyRestApi rest = _pdm.GetRestApi();
        if (rest.isConfigured()) {
            b.setEnabled(false);
            rest.DeactivatePod(result -> {
                 if (result.success) {
                    Toast("Deactivation successful", true);
                } else {
                    Toast("Deactivation failed", true);
                }
                b.setEnabled(true);
                updateGUI();
            });
        }
        else
            DisplayMessage("Need to connect to omnipy in order to perform action");

    }

    private void Shutdown(Button b){
        OmnipyRestApi rest = _pdm.GetRestApi();
        if (rest.isConfigured()) {
            b.setEnabled(false);
            rest.Shutdown (result -> {
                if (result.success) {
                    Toast("Shutdown requested", true);
                } else {
                    Toast("Shutdown request failed", true);
                }
                b.setEnabled(true);
                updateGUI();
            });
        }
        else
            DisplayMessage("Need to connect to omnipy in order to perform action");

    }

    private void Restart(Button b){
        OmnipyRestApi rest = _pdm.GetRestApi();
        if (rest.isConfigured()) {
            b.setEnabled(false);
            rest.Restart(result -> {
                if (result.success) {
                    Toast("Restart requested", true);
                } else {
                    Toast("Restart request failed", true);
                }
                b.setEnabled(true);
                updateGUI();
            });
        }
        else
            DisplayMessage("Need to connect to omnipy in order to perform action");
    }

    private void CheckRL(Button b) {
        OmnipyRestApi rest = _pdm.GetRestApi();
        if (rest.isConfigured()) {
            b.setEnabled(false);
            rest.GetRLInfo(result -> {
                if (result.success) {
                    DisplayMessage(result.response.getAsString());
                } else {
                    Toast("RL info request failed", true);
                }
                b.setEnabled(true);
                updateGUI();
            });
        }
        else
            DisplayMessage("Need to connect to omnipy in order to perform action");

    }

    private void ClearAlerts(Button b)
    {
        OmnipyRestApi rest = _pdm.GetRestApi();
        if (rest.isConfigured()) {

            b.setEnabled(false);

            rest.UpdateStatus(result -> {
                if (result.success)
                {
                    int alert = result.status.state_alert;
                    if (alert != 0)
                    {
                        rest.AcknowledgeAlerts(alert, result2->
                        {
                            if(result2.success)
                            {
                                Toast("Alerts cleared", true);
                            }
                            else
                            {
                                Toast("Failed to clear alerts", true);
                            }
                        });
                    }
                    else
                    {
                        Toast("No alerts to clear", true);
                    }
                }
                else
                {
                    Toast("Failed to update status", true);
                }

                b.setEnabled(true);
                updateGUI();
            });
        }
        else
            DisplayMessage("Need to connect to omnipy in order to perform action");
    }

    private void RegisterPod(Button b) {
        View vw = this.getView();
        EditText tx = vw.findViewById(R.id.omnipy_txt_lot_number);
        String lot = tx.getText().toString();
        tx = vw.findViewById(R.id.omnipy_txt_serial_number);
        String tid = tx.getText().toString();
        tx = vw.findViewById(R.id.omnipy_txt_serial_number);

//        CheckBox ck = vw.findViewById(R.id.omnipy_chk_read_pdm);
//        ck.isChecked()

        String rd = tx.getText().toString();
        OmnipyRestApi rest = _pdm.GetRestApi();
        if (rest.isConfigured()) {
            b.setEnabled(false);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            AlertDialog dlg = builder.setMessage("Now take your pdm in your hand and put RileyLink nearby. Click 'Status' button on your pdm" +
                    " until this message disappears")
                .show();

            rest.CreateNewPod(Integer.parseInt(lot), Integer.parseInt(tid), 0, result -> {
                dlg.dismiss();

                if (result.success) {
                    DisplayMessage("Registered new pod successfully! Now put your PDM away and don't run any other commands.");

                    AlertDialog dlgx = builder.setMessage("Trying to read pod status..")
                            .show();

                    rest.UpdateStatus(result2 -> {
                        dlgx.dismiss();
                        if (result2.success)
                        {
                            DisplayMessage("Status read from the pod successfully, pod is registered.");
                        }
                        else
                        {
                            DisplayMessage("Failed to read pod status. Try registering again or try updating the status later.");
                        }
                    });

                }
                else
                {
                    DisplayMessage("Failed to read the address from the pdm. (Did you click Status?)");
                }
                b.setEnabled(true);
            });
        }
        else
        {
            DisplayMessage("Need to connect to omnipy in order to perform action");
        }

        }


    @Override
    protected void updateGUI() {
        View vw = this.getView();

        TextView tv = vw.findViewById(R.id.omnipy_txt_connection_status);
        tv.setText(_pdm.getConnectionStatusText());

        tv = vw.findViewById(R.id.omnipy_txt_pod_status);
        tv.setText(_pdm.getPodStatusText());
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id)
        {
            case R.id.omnipy_btn_check_connection:
                CheckConnection((Button)view);
                break;
            case R.id.omnipy_btn_update_status:
                UpdateStatus((Button)view);
                break;
            case R.id.omnipy_btn_clear_alerts:
                ClearAlerts((Button)view);
                break;
            case R.id.omnipy_btn_check_rl:
                CheckRL((Button)view);
                break;
            case R.id.omnipy_btn_deactivate_pod:
                Confirm("Are you sure you want to deactivate the pod?", () -> {
                  DeactivatePod((Button)view); });
                break;
            case R.id.omnipy_btn_restart_remote_host:
                Confirm("Are you sure you want to restart the omnipy host?", () -> {
                    Restart((Button)view); });
                break;
            case R.id.omnipy_btn_shutdown_remote_host:
                Confirm("Are you sure you want to shut down the omnipy host?", () -> {
                    Shutdown((Button)view); });
                break;
            case R.id.omnipy_btn_register_pod:
                Confirm("Are you sure you want to register a new pod? This will remove your current pod.", () -> {
                    RegisterPod((Button)view); });
                break;
            default:
                break;
        }
    }

    private void DisplayMessage(String text){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(text).setPositiveButton("OK", null).show();
    }

    private void Confirm(String text, Runnable ifYes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch(i)
                {
                    case DialogInterface.BUTTON_POSITIVE:
                        ifYes.run();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        builder.setMessage(text).setPositiveButton("Yes", listener)
                .setNegativeButton("No", listener).show();
    }

}
