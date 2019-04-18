package info.nightscout.androidaps.plugins.pump.omnipod;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import info.nightscout.androidaps.R;

public class OmnipodPdmFragment extends Fragment {

    private Context context = null;

    public OmnipodPdmFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View vw =  inflater.inflate(R.layout.fragment_omnipod_pdm, container, false);
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

    }
}
