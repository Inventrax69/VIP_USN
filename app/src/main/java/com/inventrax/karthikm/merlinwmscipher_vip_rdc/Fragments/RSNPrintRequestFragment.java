package com.inventrax.karthikm.merlinwmscipher_vip_rdc.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Activities.MainActivity;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.InboundDTO;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.LSortListInventory;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.SortListDTO;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.WMSCoreMessage;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.WMSExceptionMessage;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.R;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Services.RestService;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.common.Common;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.common.constants.EndpointConstants;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.common.constants.ErrorMessages;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.interfaces.ApiInterface;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.DialogUtils;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.ExceptionLoggerUtils;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.FragmentUtils;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.ProgressDialogUtils;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.ScanValidator;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.util.SoundUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Padmaja on 04/11/2019
 */

public class RSNPrintRequestFragment extends Fragment implements View.OnClickListener, BarcodeReader.TriggerListener, BarcodeReader.BarcodeListener {
    private static final String classCode = "API_FRAG_RSNPrintRequestFragment_";
    private View rootView;

    private CardView cvScanSKU;
    private ImageView ivScanSKU;
    private RelativeLayout rlScanSKU,rlOptions;
    private LinearLayout llCType,llPType;
    private TextView txt_scannedSKU;
    private RadioGroup rgCOptions,rgPOptions;
    private RadioButton rbCReprintCSN,rbCReprintASSPSN,rbPAssociateCSN,rbPReprintPSN,rbPReprintAllPSN;
    private Button btnSubmit,btnCloseOne,btnCloseTwo;

    DialogUtils dialogUtils;

    FragmentUtils fragmentUtils;
    private Common common = null;
    String scanner = null;
    String getScanner = null;
    private IntentFilter filter;
    private ScanValidator scanValidator;
    private Gson gson;
    private WMSCoreMessage core;
    String userId = null;

    //For Honey well barcode
    private static BarcodeReader barcodeReader;
    private AidcManager manager;

    SoundUtils soundUtils = null;
    private ExceptionLoggerUtils exceptionLoggerUtils;

    ToneGenerator toneGenerator;
    private ErrorMessages errorMessages;

    private final BroadcastReceiver myDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanner = intent.getStringExtra(GeneralString.BcReaderData);  // Scanned Barcode info
            ProcessScannedinfo(scanner.trim().toString());
        }
    };

    public RSNPrintRequestFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_rsn_print_req, container, false);
        barcodeReader = MainActivity.getBarcodeObject();
        loadFormControls();
        return rootView;
    }

    // Form controls
    private void loadFormControls() {

        exceptionLoggerUtils = new ExceptionLoggerUtils();

        cvScanSKU = (CardView) rootView.findViewById(R.id.cvScanSKU);
        ivScanSKU = (ImageView) rootView.findViewById(R.id.ivScanSKU);

        rlScanSKU = (RelativeLayout) rootView.findViewById(R.id.rlScanSKU);
        rlOptions = (RelativeLayout) rootView.findViewById(R.id.rlOptions);

        txt_scannedSKU = (TextView) rootView.findViewById(R.id.txt_scannedSKU);

        llCType = (LinearLayout) rootView.findViewById(R.id.llCType);
        llPType = (LinearLayout) rootView.findViewById(R.id.llPType);

        rgCOptions = (RadioGroup) rootView.findViewById(R.id.rgCOptions);
        rgPOptions = (RadioGroup) rootView.findViewById(R.id.rgPOptions);

        rbCReprintCSN = (RadioButton) rootView.findViewById(R.id.rbCReprintCSN);
        rbCReprintASSPSN = (RadioButton) rootView.findViewById(R.id.rbCReprintASSPSN);
        rbPAssociateCSN = (RadioButton) rootView.findViewById(R.id.rbPAssociateCSN);
        rbPReprintPSN = (RadioButton) rootView.findViewById(R.id.rbPReprintPSN);
        rbPReprintAllPSN = (RadioButton) rootView.findViewById(R.id.rbPReprintAllPSN);

        btnCloseOne = (Button) rootView.findViewById(R.id.btnCloseOne);
        btnCloseTwo = (Button) rootView.findViewById(R.id.btnCloseTwo);
        btnSubmit = (Button) rootView.findViewById(R.id.btnSubmit);

        btnCloseOne.setOnClickListener(this);
        btnCloseTwo.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);


        cvScanSKU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                rlScanSKU.setVisibility(View.GONE);
                rlOptions.setVisibility(View.VISIBLE);

            }
        });

        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        userId = sp.getString("RefUserId", "");


        soundUtils = new SoundUtils();
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        common = new Common();
        errorMessages = new ErrorMessages();


        // For Cipher Barcode reader
        Intent RTintent = new Intent("sw.reader.decode.require");
        RTintent.putExtra("Enable", true);
        getActivity().sendBroadcast(RTintent);
        this.filter = new IntentFilter();
        this.filter.addAction("sw.reader.decode.complete");
        getActivity().registerReceiver(this.myDataReceiver, this.filter);

        gson = new GsonBuilder().create();
        core = new WMSCoreMessage();

        //For Honey well
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

    //button Clicks
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btnCloseOne:
                FragmentUtils.replaceFragmentWithBackStack(getActivity(),R.id.container_body,new HomeFragment());
                break;

            case R.id.btnCloseTwo:
                clearAllFields();
                break;

            case R.id.btnSubmit:

                llCType.setVisibility(View.GONE);
                llPType.setVisibility(View.VISIBLE);

                break;


            default:
                break;
        }
    }


    private void GetSortList(final String type, final String PickOrRsn) {
        try {
            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.SortListDTO, getContext());
            final SortListDTO sortListDTO = new SortListDTO();
            sortListDTO.setUserID(userId);
            if (type.equalsIgnoreCase("tripSheetNumber")) {
                sortListDTO.setTripSheetRefNo(PickOrRsn);  // "PL190202014"
            } else {
                sortListDTO.setSerialNumber(PickOrRsn);
            }
            message.setEntityObject(sortListDTO);
            Call<String> call = null;
            ApiInterface apiService = RestService.getClient().create(ApiInterface.class);
            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.GetSortList(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");
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
                common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
            }
            try {
                //Getting response from the method
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {

                        try {

                            core = gson.fromJson(response.body().toString(), WMSCoreMessage.class);
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
                                //List<LinkedTreeMap<?, ?>> _lPickRefNo = new ArrayList<LinkedTreeMap<?, ?>>();
                                LinkedTreeMap<?, ?> userMap = (LinkedTreeMap<?, ?>) core.getEntityObject();
                                // userMap = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();

                                SortListDTO lstDtolstDto = null;
                                List<SortListDTO> lstDto = new ArrayList<SortListDTO>();


                                List<LSortListInventory> pickList = new ArrayList<LSortListInventory>();
                                for (int i = 0; i < userMap.size(); i++) {
                                    lstDtolstDto = new SortListDTO(userMap.entrySet());
                                    lstDto.add(lstDtolstDto);
                                }

                                ProgressDialogUtils.closeProgressDialog();
                            }
                            ProgressDialogUtils.closeProgressDialog();


                        } catch (Exception ex) {
                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
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
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_03", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_04", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
        }
    }


    @Override
    public void onBarcodeEvent(final BarcodeReadEvent barcodeReadEvent) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // update UI to reflect the data
                // List<String> list = new ArrayList<String>();
                // list.add("Barcode data: " + barcodeReadEvent.getBarcodeData());
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

    public void HoneyWellBarcodeListeners() {

        barcodeReader.addTriggerListener(this);

        if (barcodeReader != null) {
            // set the trigger mode to client control
            barcodeReader.addBarcodeListener(this);
            try {
                barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE, BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);
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
        if (scannedData != null && !common.isPopupActive()) {
            if (scanValidator.IsPalletScanned(scannedData)) {

            } else if (ScanValidator.IsItemScanned(scannedData)) {
                GetSortList("rsn", scannedData);
            } else if (scanValidator.IsDockScanned(scannedData)) {

            }
        } else {

        }

    }

    public void clearAllFields(){

        llCType.setVisibility(View.VISIBLE);
        llPType.setVisibility(View.GONE);
        txt_scannedSKU.setText("");
        cvScanSKU.setCardBackgroundColor(getActivity().getResources().getColor(R.color.locationColor));
        ivScanSKU.setImageResource(R.drawable.fullscreen_img);
        rlOptions.setVisibility(View.GONE);
        rlScanSKU.setVisibility(View.VISIBLE);

    }

    public void ValidateLocationCode() {
        try {

            WMSCoreMessage message = new WMSCoreMessage();
            message = common.SetAuthentication(EndpointConstants.Inbound, getContext());
            InboundDTO inboundDTO = new InboundDTO();
            //  inboundDTO.setDockLocation();
            message.setEntityObject(inboundDTO);

            Call<String> call = null;
            ApiInterface apiService = RestService.getClient().create(ApiInterface.class);

            try {
                //Checking for Internet Connectivity
                // if (NetworkUtils.isInternetAvailable()) {
                // Calling the Interface method
                call = apiService.ValidateLocationCode(message);
                ProgressDialogUtils.showProgressDialog("Please Wait");
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
                                    List<LinkedTreeMap<?, ?>> _lInventory = new ArrayList<LinkedTreeMap<?, ?>>();
                                    _lInventory = (List<LinkedTreeMap<?, ?>>) core.getEntityObject();
                                    if (_lInventory.size() > 0) {
                                        ProgressDialogUtils.closeProgressDialog();
                                    } else {
                                        ProgressDialogUtils.closeProgressDialog();
                                        common.showUserDefinedAlertType(errorMessages.EMC_0028, getActivity(), getContext(), "Error");

                                        return;
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
                                logException();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ProgressDialogUtils.closeProgressDialog();
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        ProgressDialogUtils.closeProgressDialog();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                try {
                    exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
                    logException();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ProgressDialogUtils.closeProgressDialog();
                common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
            }
        } catch (Exception ex) {
            try {
                exceptionLoggerUtils.createExceptionLog(ex.toString(), classCode, "001_02", getActivity());
                logException();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0002, getActivity(), getContext(), "Error");
        }
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
                            /*try {
                                exceptionLoggerUtils.createExceptionLog(ex.toString(),classCode,"002",getContext());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            logException();*/
                            ProgressDialogUtils.closeProgressDialog();
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable throwable) {
                        ProgressDialogUtils.closeProgressDialog();
                        //Toast.makeText(LoginActivity.this, throwable.toString(), Toast.LENGTH_LONG).show();
                        common.showUserDefinedAlertType(errorMessages.EMC_0001, getActivity(), getContext(), "Error");
                    }
                });
            } catch (Exception ex) {
                ProgressDialogUtils.closeProgressDialog();
                // Toast.makeText(LoginActivity.this, ex.toString(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception ex) {
            ProgressDialogUtils.closeProgressDialog();
            common.showUserDefinedAlertType(errorMessages.EMC_0003, getActivity(), getContext(), "Error");
        }
    }


    // Update the shipment
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
            // notifications while paused.
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
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.title_activity_rsn_print));
    }

    //Barcode scanner API
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (barcodeReader != null) {
            // unregister barcode event listener honeywell
            barcodeReader.removeBarcodeListener((BarcodeReader.BarcodeListener) this);

            // unregister trigger state change listener
            barcodeReader.removeTriggerListener((BarcodeReader.TriggerListener) this);
        }

        Intent RTintent = new Intent("sw.reader.decode.require");
        RTintent.putExtra("Enable", false);
        getActivity().sendBroadcast(RTintent);
        getActivity().unregisterReceiver(this.myDataReceiver);
        super.onDestroyView();
    }


}