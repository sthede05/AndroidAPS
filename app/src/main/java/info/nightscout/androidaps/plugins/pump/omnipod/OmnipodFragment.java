package info.nightscout.androidaps.plugins.pump.omnipod;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
        if (activity != null)
            activity.runOnUiThread(() -> updateGUI() );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_omnipod, container, false);

        view.findViewById(R.id.omnipy_btn_check_connection).setOnClickListener(this);
        view.findViewById(R.id.omnipy_btn_update_status).setOnClickListener(this);
        view.findViewById(R.id.omnipy_btn_clear_alerts).setOnClickListener(this);
        view.findViewById(R.id.omnipy_btn_deactivate_pod).setOnClickListener(this);
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
                b.setEnabled(true);
                if (result.success) {
                    Toast("Status updated", true);
                } else {
                    Toast("Status update failed", true);
                }
            });
        }
        else
            Toast("Omnipy is not available", true);
    }

    private void CheckConnection(Button b) {
        OmnipyRestApi rest = _pdm.GetRestApi();
        if (rest.isConfigured()) {
            b.setEnabled(false);
            rest.CheckAuthentication(result -> {
                b.setEnabled(true);
                if (result.success) {
                    Toast("Connection successful!", true);
                } else {
                    Toast("Connection failed", true);
                }});
            }
        else
            Toast("Omnipy is not available", true);

    }

    private void ClearAlerts(Button b)
    {
        OmnipyRestApi rest = _pdm.GetRestApi();
        if (rest.isConfigured()) {
//            b.setEnabled(false);

//            rest.CheckAuthentication(result -> {
//                b.setEnabled(true);
//                if (result.success) {
//                    Toast("Connection successful!", true);
//                } else {
//                    Toast("Connection failed", true);
//                }});
        }
        else
            Toast("Omnipy is not available", true);
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
            case R.id.omnipy_btn_deactivate_pod:
                //_pdm.GetRestApi().DeactivatePod(null);
                break;
            default:
                break;
        }
    }
}
