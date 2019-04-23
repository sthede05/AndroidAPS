package info.nightscout.androidaps.plugins.pump.omnipod;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import info.nightscout.androidaps.R;
import info.nightscout.androidaps.plugins.pump.omnipod.api.OmnipodStatus;
import info.nightscout.androidaps.plugins.pump.omnipod.api.OmnipyRestApi;

import static info.nightscout.androidaps.R.*;
import static info.nightscout.androidaps.R.drawable.common_google_signin_btn_icon_dark;
import static info.nightscout.androidaps.R.drawable.ic_error;
import static info.nightscout.androidaps.R.drawable.ic_outline_check_24px;
import static info.nightscout.androidaps.R.drawable.ic_outline_close_24px;

public class OmnipodPdmFragment extends Fragment {

    private Context context = null;
    private OmnipodPdm pdm = null;
    private OmnipyRestApi rest = null;

    public OmnipodPdmFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View vw =  inflater.inflate(layout.fragment_omnipod_pdm, container, false);
        this.pdm = OmnipodPlugin.getPlugin().getPdm();
        this.rest = this.pdm.getRestApi();
        UpdateUIState(vw);
        return vw;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.context = null;
    }

    private void UpdateUIState(View vw)
    {
        OmnipodStatus status = this.pdm.getStatus();
        if (status == null || rest == null)
        {
            vw.setVisibility(View.INVISIBLE);
        }
        else
        {
            ProgressBar pv1 = vw.findViewById(id.prgConnectionStatus);
            ImageView iv1 = vw.findViewById(id.imgConnectionStatus);

            ProgressBar pv2 = vw.findViewById(id.prgRadioStatus);
            ImageView iv2 = vw.findViewById(id.imgRadioStatus);

            ProgressBar pv3 = vw.findViewById(id.prgCommandStatus);
            ImageView iv3 = vw.findViewById(id.imgCommandStatus);

            pv1.setVisibility(View.GONE);
            pv2.setVisibility(View.GONE);
            pv3.setVisibility(View.GONE);

            iv1.setVisibility(View.VISIBLE);
            iv2.setVisibility(View.VISIBLE);
            iv3.setVisibility(View.VISIBLE);

            iv1.setImageResource(ic_outline_check_24px);
            iv2.setImageResource(ic_outline_check_24px);
            iv3.setImageResource(ic_outline_check_24px);
            iv1.setImageTintList(ColorStateList.valueOf(0xff00ff00));
            iv2.setImageTintList(ColorStateList.valueOf(0xff00ff00));
            iv3.setImageTintList(ColorStateList.valueOf(0xff00ff00));

            if (!rest.isConfigured())
            {
                pv1.setVisibility(View.VISIBLE);
                iv1.setVisibility(View.GONE);
            }
            else
            {
                if (!rest.isConnectable() || !rest.isAuthenticated()) {
                    iv1.setImageResource(ic_outline_close_24px);
                    iv1.setImageTintList(ColorStateList.valueOf(0xffff0000));
                }
                else {



                }
            }
        }
    }
}
