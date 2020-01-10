package com.inventrax.karthikm.merlinwmscipher_vip_rdc.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.ColorDTO;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.InboundDTO;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.StorageLocationDTO;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.VehicleDTO;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.WMSCoreMessage;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.WMSExceptionMessage;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.SearchableSpinner.SearchableSpinner;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Services.RestService;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.common.constants.EndpointConstants;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.interfaces.ApiInterface;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.R;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.common.Common;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.common.constants.ErrorMessages;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.ExceptionLoggerUtils;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.FragmentUtils;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.NetworkUtils;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.ProgressDialogUtils;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.SoundUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Created by Prasanna.ch on 06/11/2018.
 */

public class UnloadingFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private View rootView;
    private static final String classCode = "API_FRAG_UnloadingFragment";
    private TextView txtVehicleNo, txtSelectStRef;
    private SearchableSpinner spinnerSelectVehicle, spinnerSelectStRef;
    private Button btnGo, btnCancel;
    private Gson gson;
    private SoundUtils soundUtils;
    private WMSCoreMessage core;
    private String Vehicleno = null, Storerefno = null, VehicleId = null, wareHouseID = "",tenantID="",isPalletrequired="";
    List<List<StorageLocationDTO>> sloc;
    List<List<ColorDTO>> lstColor;
    List<String> lstStorageloc;
    List<String> lstColorcodes;
    List<String> lstWareGousereId;
    List<InboundDTO> lstInbound = null;
    private Common common;
    String userId = null, VehicleReceivedQty = null, VehicleInventoryQty = null;
    String DefaultSloc = null;
    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;
    private String POHeaderID = "" , accountID = "";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (NetworkUtils.isInternetAvailable(getContext())) {
            rootView = inflater.inflate(R.layout.fragment_unloading, container, false);
            loadFormControls();
        } else {
            common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
        }
        return rootView;
    }

    public void loadFormControls() {
        if (NetworkUtils.isInternetAvailable(getContext())) {

            txtVehicleNo = (TextView) rootView.findViewById(R.id.tvVehicleNo);
            txtSelectStRef = (TextView) rootView.findViewById(R.id.tvSelectStRef);

            spinnerSelectVehicle = (SearchableSpinner) rootView.findViewById(R.id.spinnerSelectVehicle);
            spinnerSelectStRef = (SearchableSpinner) rootView.findViewById(R.id.spinnerSelectStRef);
            spinnerSelectStRef.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    Storerefno = spinnerSelectStRef.getSelectedItem().toString();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            spinnerSelectVehicle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    Vehicleno = spinnerSelectVehicle.getSelectedItem().toString();
                    GetStrorefnos();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            btnGo = (Button) rootView.findViewById(R.id.btnGo);
            btnGo.setOnClickListener(this);
            btnCancel = (Button) rootView.findViewById(R.id.btnCancel);
            btnCancel.setOnClickListener(this);

            gson = new GsonBuilder().create();
            core = new WMSCoreMessage();
            sloc = new ArrayList<>();
            lstColor = new ArrayList<>();
            lstStorageloc = new ArrayList<String>();
            lstColorcodes = new ArrayList<String>();
            lstWareGousereId = new ArrayList<String>();
            lstWareGousereId.clear();
            lstColorcodes.clear();
            lstColor.clear();
            SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
            userId = sp.getString("RefUserId", "");

            common = new Common();
            exceptionLoggerUtils = new ExceptionLoggerUtils();
            errorMessages = new ErrorMessages();
            lstInbound = new ArrayList<InboundDTO>();

            getInboundByUserId();

        } else {

            common.showUserDefinedAlertType(errorMessages.EMC_0025, getActivity(), getContext(), "Error");


            return;
        }

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btnGo:
                if (Storerefno != null) {
                    GetInboundDeatils();
                } else {
                    common.showUserDefinedAlertType(errorMessages.EMC_0044, getActivity(), getContext(), "Error");
                }
                break;
            case R.id.btnCancel:
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new HomeFragment());
                break;
            default:
                break;
        }

    }

    public void GetInboundDeatils() {
        Bundle bundle = new Bundle();
        bundle.putString("Vehicleno", Vehicleno);
        bundle.putString("Storefno", Storerefno);
        bundle.putString("wareHouseID", wareHouseID);
        bundle.putString("tenantID", tenantID);
        String SLOCjson = gson.toJson(lstStorageloc);
        bundle.putString("SLOC", SLOCjson);
        String ColorCodejson = gson.toJson(lstColorcodes);
        bundle.putString("ColorCodejson", ColorCodejson);
        bundle.putString("DefaultSLOC", DefaultSloc);
        bundle.putString("VehicleReceivedQty", VehicleReceivedQty);
        bundle.putString("VehicleInventoryQty", VehicleInventoryQty);
        bundle.putString("accountID", accountID);
        bundle.putString("POHeaderID", POHeaderID);



        if(isPalletrequired.equals("1")){
            GoodsInFragment goodsinfragment = new GoodsInFragment();
            goodsinfragment.setArguments(bundle);
            FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, goodsinfragment);
        }else{
            GoodsInWOPalletFragment goodsInWOPalletFragment = new GoodsInWOPalletFragment();
            goodsInWOPalletFragment.setArguments(bundle);
            FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, goodsInWOPalletFragment);
        }



    }

    public void getInboundByUserId() {

        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Inbound, getContext());
            InboundDTO inboundDTO = new InboundDTO();
            inboundDTO.setUserId(userId);
            message.setEntityObject(inboundDTO);


            Call<String> call = null;
            ApiInterface apiService = RestService.getClient().create(ApiInterface.class);

            try {
                // Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.GetInboundByUserId(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "GetInboundByUserId_01", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
                return;

            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        try {
                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                            if (core != null) {
                                if ((core.getType().toString().equals("Exception"))) {
                                    List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                    WMSExceptionMessage owmsExceptionMessage = null;

                                    for (int i = 0; i < _lExceptions.size(); i++) {
                                        owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                                    }

                                    ProgressDialogUtils.closeProgressDialog();
                                    common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                } else {
                                    List<LinkedTreeMap<?, ?>> _lVehicles = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lVehicles = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                    if (_lVehicles.size() > 0) {
                                        List<String> lstvehicleno = null;
                                        List<VehicleDTO> lstDto = new ArrayList<VehicleDTO>();
                                        List<String> tenantIDList = new ArrayList<String>();
                                        VehicleDTO dto = null;
                                        InboundDTO _oInbound = new InboundDTO();

                                        for (int i = 0; i < _lVehicles.size(); i++) {
                                            dto = new VehicleDTO(_lVehicles.get(i).entrySet());
                                            lstDto.add(dto);
                                        }

                                        for (int i = 0; i < lstDto.size(); i++) {
                                            if(lstDto.get(i).getColorCodes()!=null || lstDto.get(i).getSLOC()!=null || lstDto.get(i).getInboundList()!=null || lstDto.get(i).getVehicleNumber()!=null ){
                                                lstvehicleno = lstDto.get(i).getVehicleNumber();
                                                lstInbound = lstDto.get(i).getInboundList();
                                                sloc.add(lstDto.get(i).getSLOC());
                                                lstColor.add(lstDto.get(i).getColorCodes());
                                            }
                                        }
                                        // added on 28/09/2018 by Prasanna for removing duplicate vehicle no's in the list
                                        Set<String> vehicleset = new LinkedHashSet<String>(lstvehicleno);
                                        lstvehicleno.clear();
                                        lstvehicleno.addAll(vehicleset);

                                        for (int x = 0; x < sloc.size(); x++) {
                                            for (int y = 0; y < sloc.get(x).size(); y++) {
                                                if(sloc.get(x).get(y).getSLOCcode()!=null)
                                                lstStorageloc.add(sloc.get(x).get(y).getSLOCcode());
                                                if (sloc.get(x).get(y).getIsDefault().equals("True")) {
                                                    if(sloc.get(x).get(y).getSLOCcode()!=null)
                                                        DefaultSloc = sloc.get(x).get(y).getSLOCcode();
                                                }
                                            }
                                        }

                                        for (int x = 0; x < lstColor.size(); x++) {
                                            for (int y = 0; y < lstColor.get(x).size(); y++) {
                                                if(lstColor.get(x).get(y).getColorcode()!=null)
                                                lstColorcodes.add(lstColor.get(x).get(y).getColorcode());
                                            }
                                        }

                                        if(_oInbound.getVehicleReceivedQuantity()!=null || _oInbound.getVehicleInventoryQuantity() !=null ){
                                            VehicleReceivedQty = _oInbound.getVehicleReceivedQuantity();
                                            VehicleInventoryQty = _oInbound.getVehicleInventoryQuantity();
                                        }
                                        if (lstvehicleno.size() > 0) {
                                            ArrayAdapter arrayAdapter = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, lstvehicleno);
                                            spinnerSelectVehicle.setAdapter(arrayAdapter);
                                            ProgressDialogUtils.closeProgressDialog();
                                        }

                                        if( _oInbound.getPOHeaderID()!=null)
                                            POHeaderID = _oInbound.getPOHeaderID();

                                        if( _oInbound.getAccountID()!=null)
                                            accountID = _oInbound.getAccountID();

                                        if( _oInbound.getTenantID()!=null)
                                            tenantIDList.add(_oInbound.getTenantID());


                                        ProgressDialogUtils.closeProgressDialog();
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            try {
                                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "GetInboundByUserId_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ProgressDialogUtils.closeProgressDialog();
                        }

                    }

                    // response object fails
                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                        return;
                    }
                });
            } catch (Exception ex) {
                try {
                    ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "GetInboundByUserId_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                return;
            }
        } catch (Exception ex) {
            try {
                ExceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "GetInboundByUserId_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
            return;
        }
    }

    // sending exception to the database
    public void logException() {
        try {
            String textFromFile = ExceptionLoggerUtils.readFromFile(getActivity());

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Exception, getActivity());
            WMSExceptionMessage wmsExceptionMessage = new WMSExceptionMessage();
            wmsExceptionMessage.setWMSMessage(textFromFile);
            message.setEntityObject(wmsExceptionMessage);

            Call<String> call = null;
            ApiInterface apiService = RestService.getClient().create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.LogException(message);
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {

                        try {

                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                            if (core != null) {
                                // if any Exception throws
                                if ((core.getType().toString().equals("Exception"))) {
                                    List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                    WMSExceptionMessage owmsExceptionMessage = null;
                                    for (int i = 0; i < _lExceptions.size(); i++) {
                                        owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                                        common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                        ProgressDialogUtils.closeProgressDialog();
                                        return;
                                    }
                                } else {
                                    ProgressDialogUtils.closeProgressDialog();
                                    LinkedTreeMap<String, String> _lResultvalue = new LinkedTreeMap<String, String>();
                                    _lResultvalue = (LinkedTreeMap<String, String>) core.getEntityObject();
                                    for (Map.Entry<String, String> entry : _lResultvalue.entrySet()) {
                                        if (entry.getKey().equals("Result")) {
                                            String Result = entry.getValue();
                                            if (Result.equals("0")) {

                                                return;
                                            } else {
                                                exceptionLoggerUtils.deleteFile(getActivity());
                                                ProgressDialogUtils.closeProgressDialog();
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {

                            /*try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(),classCode,"002",getContext());

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            logException();*/

                            ProgressDialogUtils.closeProgressDialog();
                            Log.d("Message", core.getEntityObject().toString());
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                        return;
                    }
                });
            } catch (Exception ex) {

                // Toast.makeText(LoginActivity.this, ex.toString(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception ex) {
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            return;
        }
    }


    public void GetStrorefnos() {
        List<String> lstStorerefno = new ArrayList<>();
        for (InboundDTO oInbound : lstInbound) {
            if (oInbound.getVehicleNumber().equals(Vehicleno)) {
                lstStorerefno.add(oInbound.getStoreRefNo());
                // lstWareGousereId.add(oInbound.getWarehouseId());
                wareHouseID = oInbound.getWarehouseId();
                tenantID = oInbound.getTenantID();
                isPalletrequired = oInbound.getIsPalletrequired();
            }
        }
        ArrayAdapter arrayAdapter1 = new ArrayAdapter(getActivity(), R.layout.support_simple_spinner_dropdown_item, lstStorerefno);
        spinnerSelectStRef.setAdapter(arrayAdapter1);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {


    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onResume() {
        super.onResume();

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.title_activity_unloading));
    }
}

