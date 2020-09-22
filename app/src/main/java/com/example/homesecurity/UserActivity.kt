package com.example.homesecurity

import android.Manifest
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View

import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_user.*

const val DEVICE_NAME = "bluet"


 class UserActivity : AppCompatActivity(), BLEControl.Callback {

     //bluetooth
     private var ble: BLEControl? = null
     private var messages: TextView? = null
     private var rssiAverage:Double = 0.0
     private var player: MediaPlayer? = null
     private val REQUEST_CALL: Int = 123
     private var alertDialog: AlertDialog? = null
     private val EMERGENCY_CONTACT: String = "0000"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null) {
            if (!adapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT )

            }
        }

        // Get Bluetooth
        messages = message_textView
        messages!!.movementMethod = ScrollingMovementMethod()
        ble = BLEControl(applicationContext, DEVICE_NAME)

        // Check permissions
        ActivityCompat.requestPermissions(this,
            arrayOf( Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
            ), 1)


        // Set up listener for button

        connect_button.setOnClickListener{
            videoView.setVideoURI(Uri.parse("rtsp://192.168.0.11:8000/"))
            videoView.start()
        }
        pause_button.setOnClickListener{
            videoView.pause()
        }

    }


     private fun makeCall() {
         Toast.makeText(this, "emergency call have been made", Toast.LENGTH_SHORT).show()

         try {
             val uri: String = "tel:$EMERGENCY_CONTACT"
             val intent = Intent(Intent.ACTION_CALL, Uri.parse(uri))
             if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                 ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.CALL_PHONE) ,REQUEST_CALL)
                 return
             }
             startActivity(intent)
         } catch(e: ActivityNotFoundException) {

         }
     }
     private fun showDialog() {
         alertDialog = this.let {
             val builder = AlertDialog.Builder(it)
             builder.apply {
                 setNegativeButton("got it", DialogInterface.OnClickListener { _, _ ->
                 })
             }.setMessage("Do you want to cancel the alarm? Please press the button \"DISALARM\" at the bottom").setTitle("Alert From Arduino")

             builder.create()

         }
         alertDialog?.show()
     }

     private fun showDialog2() {
         alertDialog = this.let {
             val builder = AlertDialog.Builder(it)
             builder.apply {
                 setNegativeButton("got it", DialogInterface.OnClickListener { _, _ ->
                     player?.stop()
                 })
             }.setMessage("Do you want to turn up the heat? \n WINTER IS COMING!!!!")
                 .setTitle("Alert From Arduino")

             builder.create()
         }
         alertDialog?.show()
     }


     //Function that reads the RSSI value associated with the bluetooth connection between the phone and the Arduino board
     //If you use the RSSI to calculate distance, you may want to record a set of values over a period of time
     //and obtain the average

     fun clearText (v: View){
         messages!!.text = ""

     }

     override fun onResume() {
         super.onResume()
         //updateButtons(false)
         ble!!.registerCallback(this)
     }

     override fun onStop() {
         super.onStop()
         ble!!.unregisterCallback(this)
         ble!!.disconnect()
     }

     fun connect(v: View) {
         startScan()
     }

     private fun startScan() {
         writeLine("Scanning for devices ...")
         ble!!.connectFirstAvailable()

     }


     /**
      * Press button to receive the temperature value form the board
      */
     fun readTemp(v: View) {
         ble!!.send("readtemp")
         Log.i("BLE", "READ TEMP")
     }

     /**
      * Press button to disAlarm
      */

     fun disAlarm(v: View) {
         ble!!.send("cancel")
         player!!.stop()
     }

     fun activate (v: View) {
         ble!!.send("activate")
     }

     fun deActivate(v: View) {
         ble!!.send("deactivate")
     }


     /**
      * Writes a line to the messages textbox
      * @param text: the text that you want to write
      */
     private fun writeLine(text: CharSequence) {
         runOnUiThread {
             messages!!.append(text)
             messages!!.append("\n")
         }
     }

     /**
      * Called when a UART device is discovered (after calling startScan)
      * @param device: the BLE device
      */
     override fun onDeviceFound(device: BluetoothDevice) {
         writeLine("Found device : " + device.name)
         writeLine("Waiting for a connection ...")
     }

     /**
      * Prints the devices information
      */
     override fun onDeviceInfoAvailable() {
         writeLine(ble!!.deviceInfo)
     }

     override fun onRSSIread(uart: BLEControl, rssi: Int) {
         rssiAverage = rssi.toDouble()
         writeLine("RSSI $rssiAverage")
     }

     /**
      * Called when UART device is connected and ready to send/receive data
      * @param ble: the BLE UART object
      */
     override fun onConnected(ble: BLEControl) {
         writeLine("Connected!")

     }

     /**
      * Called when some error occurred which prevented UART connection from completing
      * @param ble: the BLE UART object
      */
     override fun onConnectFailed(ble: BLEControl) {
         writeLine("Error connecting to device!")
     }

     /**
      * Called when the UART device disconnected
      * @param ble: the BLE UART object
      */
     override fun onDisconnected(ble: BLEControl) {
         writeLine("Disconnected!")

     }

     /**
      * Called when data is received by the UART
      * @param ble: the BLE UART object
      * @param rx: the received characteristic
      */
     @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
     override fun onReceive(ble: BLEControl, rx: BluetoothGattCharacteristic) {
         when {
             rx.getStringValue(0) == "door open!!" -> {
                 runOnUiThread {
                     showDialog()
                     player = MediaPlayer.create(this, R.raw.audio1)
                     player!!.start()
                 }
             }
             rx.getStringValue(0) == "times up!" -> {
                 runOnUiThread {
                     makeCall()
                 }
             }
             rx.getStringValue(0) == "too cold!" -> {
                 runOnUiThread{
                     showDialog2()
                     player = MediaPlayer.create(this, R.raw.audio2)
                     player!!.start()
                 }
             }
         }
         writeLine("Received value: " + rx.getStringValue(0))


     }

    companion object {
        private const val REQUEST_ENABLE_BT = 0
    }

}