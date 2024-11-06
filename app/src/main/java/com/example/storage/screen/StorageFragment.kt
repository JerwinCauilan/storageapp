package com.example.storage.screen

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.storage.R
import com.example.storage.databinding.FragmentStorageBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class StorageFragment : Fragment() {
    private lateinit var binding : FragmentStorageBinding
    private val db = FirebaseFirestore.getInstance()
    private val productExpiryDays = mapOf(
        "Pork" to 5,
        "Chicken" to 2,
        "Root vegetable" to 7,
        "Beef" to 5,
        "Leafy green" to 5,
        "Bell peppers" to 5,
        "Cucumbers" to 5,
        "Zucchini" to 5,
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStorageBinding.inflate(inflater, container, false)

        addHeaderRow()
        fetchData()

        binding.btnAdd.setOnClickListener { addItemDialog() }

        return binding.root
    }

    private fun addItemDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_item, null)
        val productSpinner: Spinner = dialogView.findViewById(R.id.spinnerProduct)
        val qtyET: EditText = dialogView.findViewById(R.id.quantityET)

        val productList = listOf("Select a product", "Pork", "Chicken", "Root vegetable", "Beef", "Leafy green", "Bell peppers", "Cucumbers", "Zucchini")
        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, productList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view as TextView
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))
                view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        productSpinner.adapter = adapter

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val selected = productSpinner.selectedItem.toString()
            val qty = qtyET.text.toString()

            if (selected == "Select a product") {
                Toast.makeText(requireContext(), "Please select a product", Toast.LENGTH_SHORT).show()
            } else if (qty.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a quantity", Toast.LENGTH_SHORT).show()
            } else {
                storeData(selected, qty)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun storeData(product: String, quantity: String) {
        val currentDate = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(Date())
        val expiryDays = productExpiryDays[product] ?: 1
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, expiryDays)
        val expiryDate = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(calendar.time)
        val timestamp = System.currentTimeMillis()

        val data = hashMapOf(
            "purchaseDate" to currentDate,
            "product" to product,
            "quantity" to quantity,
            "expiryDate" to expiryDate,
            "timestamp" to timestamp
        )

        db.collection("storage")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Product added successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addRowToTable(purchaseDate: String, product: String, quantity: String, expiryDate: String) {
        val tableRow = TableRow(requireContext())
        val rowData = listOf(purchaseDate, product, quantity, expiryDate)

        tableRow.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray_100))
        rowData.forEachIndexed { index, cellData ->
            val cell = TextView(requireContext())
            cell.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            if (index == 2) {
                cell.gravity = Gravity.CENTER
            }

            cell.text = cellData
            cell.setPadding(32, 16, 32, 16)
            cell.textSize = 14f
            cell.typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto)
            cell.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            tableRow.addView(cell)
        }

        binding.tableLayout.addView(tableRow)
    }

    private fun addHeaderRow() {
        val headerRow = TableRow(requireContext())
        val headers = listOf("Purchase Date", "Product", "Quantity", "Expiry Date")

        headers.forEach { header ->
            val headerCell = TextView(requireContext())
            headerCell.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            headerCell.setPadding(32, 16, 32, 16)
            headerCell.text = header
            headerCell.textSize = 16f
            headerCell.typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_medium)
            headerCell.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_500))
            headerCell.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            headerRow.addView(headerCell)
        }

        binding.tableLayout.addView(headerRow)
    }

    private fun fetchData() {
        db.collection("storage")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Toast.makeText(requireContext(), "Unexpected error", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    binding.tableLayout.removeViews(1, binding.tableLayout.childCount - 1)

                    displayData(snapshot)
                } else {
                    Log.d("StorageFragment", "Current data: null")
                }
            }
    }

    private fun displayData(documents: QuerySnapshot) {
        for (document in documents) {
            val purchaseDate = document.getString("purchaseDate") ?: ""
            val product = document.getString("product") ?: ""
            val quantity = document.getString("quantity") ?: ""
            val expiryDate = document.getString("expiryDate") ?: ""

            addRowToTable(purchaseDate, product, quantity, expiryDate)
        }
    }

}