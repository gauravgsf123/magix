package com.mpcl.activity.operation.ekart

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import cn.pedant.SweetAlert.SweetAlertDialog
import com.mpcl.R
import com.mpcl.activity.operation.box_wise_scan.BoxWiseScanAdapter
import com.mpcl.activity.operation.box_wise_scan.BoxWiseScanRequestModel
import com.mpcl.activity.operation.boxpacking.BoxPackingAdapter
import com.mpcl.app.BaseActivity
import com.mpcl.app.Constant
import com.mpcl.app.ManagePermissions
import com.mpcl.database.EkartDB
import com.mpcl.database.EkartDB_
import com.mpcl.database.ObjectBox
import com.mpcl.databinding.ActivityBoxWiseScanBinding
import com.mpcl.databinding.ActivityFlipkartBinding
import com.mpcl.model.EkartResponseModel
import com.mpcl.util.qr_scanner.QRcodeScanningActivity
import com.mpcl.viewmodel.EKartViewModel.EkartRepository
import com.mpcl.viewmodel.EKartViewModel.EkartViewModel
import com.mpcl.viewmodel.EKartViewModel.EkartViewModelFactory
import io.objectbox.Box

class FlipkartActivity : BaseActivity() {
    private lateinit var binding:ActivityFlipkartBinding
    private val REQUEST_CAMERA_CAPTURE = 100
    private lateinit var managePermissions: ManagePermissions
    private var selectedScanningSDK = QRcodeScanningActivity.ScannerSDK.MLKIT
    private val permissionList = listOf(
        Manifest.permission.CAMERA
    )
    private var isCamera = false
    private var barCodeList = ArrayList<String>()
    private var type = "IN"
    private lateinit var ekartRepository: EkartRepository
    private lateinit var ekartViewModel: EkartViewModel
    private lateinit var ekartViewModelFactory: EkartViewModelFactory
    private var eKartList: List<EkartResponseModel>? = null
    private var eKartBox: Box<EkartDB>? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlipkartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.ivHome.setOnClickListener {
            onBackPressed()
        }
        eKartBox = ObjectBox.boxStore.boxFor(EkartDB::class.java)
        managePermissions = ManagePermissions(this, permissionList, Constant.REQUEST_PERMISION)
        ekartRepository = EkartRepository()
        ekartViewModelFactory = EkartViewModelFactory(ekartRepository)
        ekartViewModel =
            ViewModelProvider(this, ekartViewModelFactory).get(EkartViewModel::class.java)
        binding.boxPackingListRecyclerview.adapter = EkartAdapter()
        binding.ivCamera.setOnClickListener {
            isCamera = true
            managePermissions.checkPermissions()
            selectedScanningSDK = QRcodeScanningActivity.ScannerSDK.MLKIT
            startScanning()
        }
        binding.btnSubmit.setOnClickListener {
            validate()
        }
        binding.btnNewScan.setOnClickListener {
            barCodeList.clear()
            (binding.boxPackingListRecyclerview.adapter as BoxWiseScanAdapter).setItems(barCodeList,this)
        }

        binding.scanBarCode.setOnTouchListener { v, event ->
            v.onTouchEvent(event)
            val inputMethod: InputMethodManager =
                v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (inputMethod != null) {
                inputMethod.hideSoftInputFromWindow(v.windowToken, 0)
            }
            true
        }
        ekartViewModel.eKartResponse.observe(this, Observer {
            hideDialog()
            val responseModel = it ?: return@Observer
            eKartList = responseModel
            //Log.d(TAG, Gson().toJson(responseModel))
            createTable(responseModel)
        })
        showDialog()
        ekartViewModel.getEKartList()
        setObservar()
    }

    private fun setObservar() {

        ekartViewModel.eKartResponse.observe(this, Observer {
            hideDialog()
            val responseModel = it ?: return@Observer
            eKartList = responseModel
            //Log.d(TAG, Gson().toJson(responseModel))
            createTable(responseModel)
        })

    }

    private fun clearAllData(){
        barCodeList.clear()
        (binding.boxPackingListRecyclerview.adapter as EkartAdapter).setItems(barCodeList,this)
        binding.run {
            vehicleNo.setText("")
            scanBarCode.setText("")
            scanBarCode.requestFocus()
        }

    }

    private fun validate(){
        if(TextUtils.isEmpty(binding?.vehicleNo.text.toString())){
            Toast.makeText(this,"Please enter vehicle number", Toast.LENGTH_SHORT).show()
        }else{
            showDialog()
            var dataRequest = ArrayList<BoxWiseScanRequestModel.Data>()
            for (value in barCodeList) {
                dataRequest.add(BoxWiseScanRequestModel.Data(value))
            }

            var requestModel = BoxWiseScanRequestModel(
                sharedPreference.getValueString(Constant.COMPANY_ID)!!,
                sharedPreference.getValueString(Constant.BID)!!,
                binding?.vehicleNo.text.toString(),
                type,
                dataRequest!!
            )
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        /*if (sharedPreference.getValueString("result")!!.isNotEmpty()) {
            var str = sharedPreference.getValueString("result")
            Log.d(TAG, str!!)
            binding.scanBarCode.setText(str)
            sharedPreference.removeValue("result")
            barCodeList.add(str)
            (binding.boxPackingListRecyclerview.adapter as BoxWiseScanAdapter).setItems(barCodeList,this)
            Toast.makeText(this,"${binding.scanBarCode.text.toString()} Added", Toast.LENGTH_SHORT).show()
            binding.scanBarCode.requestFocus()
            binding.scanBarCode.setText("")
            //if(isCamera) startScanning()
        }*/
        if (sharedPreference.getValueString("result") != null) {
            var scanValue = sharedPreference.getValueString("result")!!
            binding.scanBarCode.setText(scanValue)
            (binding.boxPackingListRecyclerview.adapter as EkartAdapter).setItems(barCodeList,this)
            var barcode = scanValue.split("-")
            var barCodeEKart = eKartBox?.query(EkartDB_.lcode.equal(barcode[0]))?.build()?.findFirst()
            if (barCodeEKart?.location?.isNullOrEmpty() == false) {
                if (barCodeEKart?.srno != null) {
                    var str = "${barCodeEKart?.srno}. ${barCodeEKart?.location}"
                    barCodeList.add(str)
                    (binding.boxPackingListRecyclerview.adapter as EkartAdapter).setItems(barCodeList,this)
                    Toast.makeText(this,"${binding.scanBarCode.text.toString()} Added", Toast.LENGTH_SHORT).show()
                }else {
                    Toast.makeText(this,"Not readable",Toast.LENGTH_SHORT).show()
                }
            } else {
                var QRcode = scanValue.split("|")
                QRcode.forEach {
                    if (it.length == 7) {
                        var ekart = eKartBox?.query(EkartDB_.lcode.equal(it))?.build()?.findFirst()
                        ekart?.location?.let { it1 -> Log.d(TAG, it1) }
                        //routeBox?.query()?.equal(RouteDB_.RouteID, id)?.build()?.findFirst()
                        if (ekart?.srno != null) {
                            var str = "${ekart?.srno}. ${ekart?.location}"
                            barCodeList.add(str)
                            (binding.boxPackingListRecyclerview.adapter as EkartAdapter).setItems(barCodeList,this)
                            Toast.makeText(this,"${binding.scanBarCode.text.toString()} Added", Toast.LENGTH_SHORT).show()

                        } else {
                            Toast.makeText(this,"Not readable",Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            }


            sharedPreference.removeValue("result")
        }
    }

    private fun startScanning() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCameraWithScanner()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_CAPTURE
            )
        }
    }

    private fun openCameraWithScanner() {
        QRcodeScanningActivity.start(this, selectedScanningSDK)
    }

    private fun createTable(responseModel: List<EkartResponseModel>) {
        val iterator = responseModel.listIterator()
        var _id: MutableList<String> = mutableListOf()

        for (t in responseModel) {
            t.lcode?.let { _id.add(it) }
        }
        eKartBox?.removeAll()
        for (data in responseModel) {
            eKartBox?.put(getEkartData(data))
        }
    }

    fun getEkartData(item: EkartResponseModel): EkartDB {
        val ekartDB = EkartDB()
        ekartDB.srno = item.srno
        ekartDB.lcode = item.lcode
        ekartDB.location = item.location

        return ekartDB
    }
}