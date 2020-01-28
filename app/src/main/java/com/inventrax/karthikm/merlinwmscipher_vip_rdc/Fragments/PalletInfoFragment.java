package com.inventrax.karthikm.merlinwmscipher_vip_rdc.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cipherlab.barcode.GeneralString;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.ScannerUnavailableException;
import com.honeywell.aidc.TriggerStateChangeEvent;
import com.honeywell.aidc.UnsupportedPropertyException;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.InventoryDTO;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.WMSCoreMessage;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.WMSExceptionMessage;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.R;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Services.RestService;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.adapters.PalletInfoAdapter;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.common.Common;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.common.constants.EndpointConstants;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.common.constants.ErrorMessages;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.interfaces.ApiInterface;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.DialogUtils;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.ExceptionLoggerUtils;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.FragmentUtils;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.ProgressDialogUtils;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.ScanValidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PalletInfoFragment extends Fragment implements View.OnClickListener, BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener {


    private static final String classCode = "API_FRAG_PalletInfoFragment";
    private View rootView;

    String scanner = null;
    private IntentFilter filter;
    private WMSCoreMessage core;
    String getScanner = null;
    private Gson gson;

    private static BarcodeReader barcodeReader;
    private AidcManager manager;
    private Common common;

    private RelativeLayout rlScan, rlPalletInfo;
    private TextView lblScannedData, tvPalletNoScanned, tvMCode, MDesc, tvLocation;
    private CardView cvScanPallet;
    private ImageView ivScanPallet;
    private Button btnClose, btnCloseInfo;
    private RecyclerView recycler_view_palletInfo;
    private LinearLayoutManager linearLayoutManager;

    private ExceptionLoggerUtils exceptionLoggerUtils;
    private ErrorMessages errorMessages;

    private String userId = "";


    // Cipher Barcode Scanner
    private final BroadcastReceiver myDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            scanner = intent.getStringExtra(GeneralString.BcReaderData);  // Scanned Barcode info
            ProcessScannedinfo(scanner.trim().toString());
        }
    };

    public PalletInfoFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_pallet_info, container, false);
        loadFormControls();

        return rootView;
    }

    private void loadFormControls() {

        rlScan = (RelativeLayout) rootView.findViewById(R.id.rlScan);
        rlPalletInfo = (RelativeLayout) rootView.findViewById(R.id.rlPalletInfo);

        cvScanPallet = (CardView) rootView.findViewById(R.id.cvScanPallet);

        ivScanPallet = (ImageView) rootView.findViewById(R.id.ivScanPallet);

        btnClose = (Button) rootView.findViewById(R.id.btnClose);
        btnCloseInfo = (Button) rootView.findViewById(R.id.btnCloseInfo);

        lblScannedData = (TextView) rootView.findViewById(R.id.lblScannedData);
        tvPalletNoScanned = (TextView) rootView.findViewById(R.id.tvPalletNoScanned);
        tvMCode = (TextView) rootView.findViewById(R.id.tvMCode);
        MDesc = (TextView) rootView.findViewById(R.id.MDesc);
        tvLocation = (TextView) rootView.findViewById(R.id.tvLocation);

        recycler_view_palletInfo = (RecyclerView) rootView.findViewById(R.id.recycler_view_palletInfo);
        recycler_view_palletInfo.setHasFixedSize(true);

        linearLayoutManager = new LinearLayoutManager(getContext());

        // use a linear layout manager
        recycler_view_palletInfo.setLayoutManager(linearLayoutManager);

        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        userId = sp.getString("RefUserId", "");

        exceptionLoggerUtils = new ExceptionLoggerUtils();
        errorMessages = new ErrorMessages();

        // For Cipher Barcode reader
        Intent RTintent = new Intent("sw.reader.decode.require");
        RTintent.putExtra("Enable", true);
        getActivity().sendBroadcast(RTintent);
        this.filter = new IntentFilter();
        this.filter.addAction("sw.reader.decode.complete");
        getActivity().registerReceiver(this.myDataReceiver, this.filter);


        common = new Common();
        gson = new GsonBuilder().create();

        btnClose.setOnClickListener(this);
        btnCloseInfo.setOnClickListener(this);

        //For Honeywell
        AidcManager.create(getActivity(), new AidcManager.CreatedCallback() {

            @Override
            public void onCreated(AidcManager aidcManager) {

                manager = aidcManager;
                barcodeReader = manager.createBarcodeReader();
                try {
                    barcodeReader.claim();
                    HoneyWellBarcodeListeners();

                } catch (ScannerUnavailableException e) {
                    e.printStackTrace();
                }
            }
        });

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnClose:
                FragmentUtils.replaceFragment(getActivity(), R.id.container_body, new HomeFragment());
                break;

            case R.id.btnCloseInfo:
                rlScan.setVisibility(View.VISIBLE);
                rlPalletInfo.setVisibility(View.GONE);

                clearFields();

                break;



        }
    }


    // honeywell
    @Override
    public void onBarcodeEvent(final BarcodeReadEvent barcodeReadEvent) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // update UI to reflect the data
                getScanner = barcodeReadEvent.getBarcodeData();
                ProcessScannedinfo(getScanner);

            }

        });
    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent) {

    }

    @Override
    public void onTriggerEvent(TriggerStateChangeEvent triggerStateChangeEvent) {

    }

    //Honeywell Barcode reader Properties
    public void HoneyWellBarcodeListeners() {

        barcodeReader.addTriggerListener(this);

        if (barcodeReader != null) {
            // set the trigger mode to client control
            barcodeReader.addBarcodeListener(this);
            try {
                barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                        BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);
            } catch (UnsupportedPropertyException e) {
                // Toast.makeText(this, "Failed to apply properties", Toast.LENGTH_SHORT).show();
            }

            Map<String, Object> properties = new HashMap<String, Object>();
            // Set Symbologies On/Off
            properties.put(BarcodeReader.PROPERTY_CODE_128_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_GS1_128_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_QR_CODE_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_CODE_39_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_UPC_A_ENABLE, true);
            properties.put(BarcodeReader.PROPERTY_EAN_13_ENABLED, false);
            properties.put(BarcodeReader.PROPERTY_AZTEC_ENABLED, false);
            properties.put(BarcodeReader.PROPERTY_CODABAR_ENABLED, false);
            properties.put(BarcodeReader.PROPERTY_INTERLEAVED_25_ENABLED, false);
            properties.put(BarcodeReader.PROPERTY_PDF_417_ENABLED, false);
            // Set Max Code 39 barcode length
            properties.put(BarcodeReader.PROPERTY_CODE_39_MAXIMUM_LENGTH, 10);
            // Turn on center decoding
            properties.put(BarcodeReader.PROPERTY_CENTER_DECODE, true);
            // Enable bad read response
            properties.put(BarcodeReader.PROPERTY_NOTIFICATION_BAD_READ_ENABLED, true);
            // Apply the settings
            barcodeReader.setProperties(properties);
        }
    }


    //Assigning scanned value to the respective fields
    public void ProcessScannedinfo(String scannedData) {
        if (scannedData != null) {

            if (ScanValidator.IsItemScanned(scannedData)) {
                lblScannedData.setText(scannedData);


                ProgressDialogUtils.showProgressDialog("Please Wait..");
                GetCartonSerialNumberInfo();


                return;

            }
        }
    }


    public void GetCartonSerialNumberInfo() {
        try {
            if (lblScannedData.getText().toString().isEmpty()) {
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0022);
                return;
            }


            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Inventory, getContext());
            InventoryDTO inventoryDTO = new InventoryDTO();
            inventoryDTO.setUserId(Integer.parseInt(userId));
            inventoryDTO.setRSN(lblScannedData.getText().toString());
            message.setEntityObject(inventoryDTO);


            Call<String> call = null;
            ApiInterface apiService =
                    RestService.getClient().create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                ProgressDialogUtils.showProgressDialog("Please Wait");
                call = apiService.GetCartonSerialNumberInfo(message);
                // } else {
                // DialogUtils.showAlertDialog(getActivity(), "Please enable internet");
                // return;
                // }

            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_01", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0002);
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.body() != null) {
                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
                            if ((core.getType().toString().equals("Exception"))) {
                                List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                WMSExceptionMessage owmsExceptionMessage = null;

                                for (int i = 0; i < _lExceptions.size(); i++) {

                                    owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                                    ProgressDialogUtils.closeProgressDialog();

                                    cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                    ivScanPallet.setImageResource(R.drawable.warning_img);
                                    common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                    recycler_view_palletInfo.setAdapter(null);
                                    lblScannedData.setText("");
                                    ProgressDialogUtils.closeProgressDialog();
                                    return;

                                }


                            } else {


                                List<LinkedTreeMap<?, ?>> _lInventory = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lInventory = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                List<InventoryDTO> lstInventory = new ArrayList<InventoryDTO>();

                                InventoryDTO inventorydto = null;
                                for (int i = 0; i < _lInventory.size(); i++) {

                                    inventorydto = new InventoryDTO(_lInventory.get(i).entrySet());
                                    lstInventory.add(inventorydto);
                                }
                                ProgressDialogUtils.closeProgressDialog();


                                rlPalletInfo.setVisibility(View.VISIBLE);
                                rlScan.setVisibility(View.GONE);

                                tvPalletNoScanned.setText(lblScannedData.getText().toString());
                                tvMCode.setText(lstInventory.get(0).getMaterialCode());
                                MDesc.setText(lstInventory.get(0).getMaterialShortDescription());
                                tvLocation.setText(lstInventory.get(0).getLocationCode());

                                PalletInfoAdapter rsntrackadapter = new PalletInfoAdapter(getActivity(), lstInventory);
                                recycler_view_palletInfo.setAdapter(rsntrackadapter);

                                cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.white));
                                ivScanPallet.setImageResource(R.drawable.check);


                                return;


                            }
                        } else {
                            recycler_view_palletInfo.setAdapter(null);
                            ProgressDialogUtils.closeProgressDialog();
                            DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0002);

                            return;
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {

                        recycler_view_palletInfo.setAdapter(null);
                        ProgressDialogUtils.closeProgressDialog();
                        DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);

                        return;
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                recycler_view_palletInfo.setAdapter(null);
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);


            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }

            ProgressDialogUtils.closeProgressDialog();
            DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0003);
            return;
        }
    }

    private void clearFields(){

        cvScanPallet.setCardBackgroundColor(getResources().getColor(R.color.palletColor));
        ivScanPallet.setImageResource(R.drawable.fullscreen_img);

        recycler_view_palletInfo.setAdapter(null);
        lblScannedData.setText("");
        tvLocation.setText("");
        tvMCode.setText("");
        MDesc.setText("");
        tvPalletNoScanned.setText("");

    }


    // sending exception to the database
    public void logException() {
        try {

            String textFromFile = exceptionLoggerUtils.readFromFile(getActivity());

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Exception, getActivity());
            WMSExceptionMessage wmsExceptionMessage = new WMSExceptionMessage();
            wmsExceptionMessage.setWMSMessage(textFromFile);
            message.setEntityObject(wmsExceptionMessage);

            Call<String> call = null;
            ApiInterface apiService =
                    RestService.getClient().create(ApiInterface.class);

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
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_01", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0002);
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {

                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {

                        try {

                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);

                            // if any Exception throws
                            if ((core.getType().toString().equals("Exception"))) {
                                List<LinkedTreeMap<?, ?>> _lExceptions = new ArrayList<LinkedTreeMap<?, ?>>();
                                _lExceptions = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                WMSExceptionMessage owmsExceptionMessage = null;
                                for (int i = 0; i < _lExceptions.size(); i++) {
                                    owmsExceptionMessage = new WMSExceptionMessage(_lExceptions.get(i).entrySet());
                                    ProgressDialogUtils.closeProgressDialog();
                                    common.showAlertType(owmsExceptionMessage, getActivity(), getContext());
                                    return;
                                }
                            } else {
                                LinkedTreeMap<String, String> _lResultvalue = new LinkedTreeMap<String, String>();
                                _lResultvalue = (LinkedTreeMap<String, String>) core.getEntityObject();
                                for (Map.Entry<String, String> entry : _lResultvalue.entrySet()) {
                                    if (entry.getKey().equals("Result")) {
                                        String Result = entry.getValue();
                                        if (Result.equals("0")) {
                                            ProgressDialogUtils.closeProgressDialog();
                                            return;
                                        } else {
                                            ProgressDialogUtils.closeProgressDialog();
                                            exceptionLoggerUtils.deleteFile(getActivity());
                                            return;
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {

                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ProgressDialogUtils.closeProgressDialog();

                            //Log.d("Message", core.getEntityObject().toString());
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0001);
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0002);
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "002_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            DialogUtils.showAlertDialog(getActivity(), errorMessages.EMC_0003);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        if (barcodeReader != null) {
            // release the scanner claim so we don't get any scanner
            try {
                barcodeReader.claim();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
            }
            barcodeReader.release();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (barcodeReader != null) {
            try {
                barcodeReader.claim();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
                // Toast.makeText(this, "Scanner unavailable", Toast.LENGTH_SHORT).show();
            }
        }
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.title_activity_palletInfo));
    }


    @Override
    public void onDestroyView() {

        // Honeywell onDestroyView
        if (barcodeReader != null) {
            // unregister barcode event listener honeywell
            barcodeReader.removeBarcodeListener((BarcodeReader.BarcodeListener) this);

            // unregister trigger state change listener
            barcodeReader.removeTriggerListener((BarcodeReader.TriggerListener) this);
        }

        // Cipher onDestroyView
        Intent RTintent = new Intent("sw.reader.decode.require");
        RTintent.putExtra("Enable", false);
        getActivity().sendBroadcast(RTintent);
        getActivity().unregisterReceiver(this.myDataReceiver);
        super.onDestroyView();

    }
}
