package org.airella.airella.ui.station.config

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.fragment_station_config.*
import org.airella.airella.R
import org.airella.airella.utils.Log

class StationConfigFragment : Fragment() {

    private val toast by lazy { Toast.makeText(requireContext(), "", Toast.LENGTH_LONG) }

    private val btBondBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent) {
            updateBondState()
            when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)) {
                BluetoothDevice.BOND_NONE -> Log.i("Bonding Failed")
                BluetoothDevice.BOND_BONDING -> Log.i("Bonding...")
                BluetoothDevice.BOND_BONDED -> Log.i("Bonded!")
            }
        }
    }

    private lateinit var viewModel: StationConfigViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this).get(StationConfigViewModel::class.java)

        viewModel.btDevice = requireArguments().getParcelable("bt_device") as BluetoothDevice

        return inflater.inflate(R.layout.fragment_station_config, container, false)
    }

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateBondState()

        viewModel.status.observe(viewLifecycleOwner, Observer {
            toast.setText(it)
            toast.show()
        })

        bond_device.setOnClickListener {
            viewModel.btDevice.createBond()
            updateBondState()
            requireActivity().registerReceiver(
                btBondBroadcastReceiver,
                IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            )
        }

        wifi_config.setOnClickListener { v ->
            val form =
                requireActivity().layoutInflater.inflate(R.layout.view_device_wifi_config, null)
            AlertDialog.Builder(requireContext())
                .setMessage("Wifi config")
                .setView(form)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    val ssid = form.findViewById<EditText>(R.id.wifiSSID).text.toString()
                    val pass = form.findViewById<EditText>(R.id.wifiPassword).text.toString()
                    viewModel.saveWiFiConfig(requireContext(), ssid, pass)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }


        address_config.setOnClickListener {
            val form =
                requireActivity().layoutInflater.inflate(R.layout.view_device_address_config, null)
            AlertDialog.Builder(requireContext())
                .setMessage("Address config")
                .setView(form)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    val stationName = form.findViewById<EditText>(R.id.stationName).text.toString()
                    val country = form.findViewById<EditText>(R.id.country).text.toString()
                    val city = form.findViewById<EditText>(R.id.city).text.toString()
                    val street = form.findViewById<EditText>(R.id.street).text.toString()
                    val houseNo = form.findViewById<EditText>(R.id.houseNo).text.toString()
                    viewModel.saveAddress(
                        requireContext(),
                        stationName,
                        country,
                        city,
                        street,
                        houseNo
                    )
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }


        location_config.setOnClickListener {
            val form =
                requireActivity().layoutInflater.inflate(R.layout.view_device_location_config, null)
            val autoLoc: CheckBox = form.findViewById(R.id.locationAuto)
            val latitude: TextInputEditText = form.findViewById(R.id.latitude)
            val longitude: TextInputEditText = form.findViewById(R.id.longitude)
            autoLoc.setOnCheckedChangeListener { _, isChecked ->
                form.findViewById<TextInputLayout>(R.id.latitudeLayout).isEnabled = !isChecked
                form.findViewById<TextInputLayout>(R.id.longitudeLayout).isEnabled = !isChecked
                latitude.isEnabled = !isChecked
                longitude.isEnabled = !isChecked
                if (isChecked) {
                    latitude.setText("50.064")
                    longitude.setText("19.944")
                }
            }
            AlertDialog.Builder(requireContext())
                .setMessage("Location config")
                .setView(form)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    viewModel.saveLocation(
                        requireContext(),
                        latitude.text.toString(),
                        longitude.text.toString()
                    )
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        change_password.setOnClickListener {
            val form = requireActivity().layoutInflater.inflate(R.layout.view_device_password, null)
            AlertDialog.Builder(requireContext())
                .setMessage("Password change")
                .setView(form)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    val newPass = form.findViewById<EditText>(R.id.password).text.toString()
                    viewModel.saveStationPassword(requireContext(), newPass)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        hard_reset.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Hard Reset")
                .setMessage("Are you sure you want to reset all configuration?")
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    viewModel.hardResetDevice(requireContext())
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateBondState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            requireActivity().unregisterReceiver(btBondBroadcastReceiver)
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun switchFragment(view: View, fragment: Fragment) {
        val transaction: FragmentTransaction =
            (view.context as FragmentActivity).supportFragmentManager.beginTransaction()

        val bundle = Bundle()
        bundle.putParcelable("bt_device", viewModel.btDevice)
        fragment.arguments = bundle

        transaction.replace(R.id.container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun updateBondState() {
        bond_status.text = when (viewModel.btDevice.bondState) {
            BluetoothDevice.BOND_NONE -> "NOT BONDED"
            BluetoothDevice.BOND_BONDING -> "BONDING"
            else -> "BONDED"
        }
        bond_status.setTextColor(
            when (viewModel.btDevice.bondState) {
                BluetoothDevice.BOND_NONE -> Color.RED
                BluetoothDevice.BOND_BONDING -> Color.rgb(255, 140, 0)
                else -> Color.GREEN
            }
        )

        bond_device.isEnabled = viewModel.btDevice.bondState == BluetoothDevice.BOND_NONE

        wifi_config.isEnabled = viewModel.isBonded()
        address_config.isEnabled = viewModel.isBonded()
        location_config.isEnabled = viewModel.isBonded()
        change_password.isEnabled = viewModel.isBonded()
        hard_reset.isEnabled = viewModel.isBonded()

//        bond_device.isEnabled = false
//
//        wifi_config.isEnabled = true
//        address_config.isEnabled = true
//        location_config.isEnabled = true
//        change_password.isEnabled = true
//        hard_reset.isEnabled = true
    }

}

