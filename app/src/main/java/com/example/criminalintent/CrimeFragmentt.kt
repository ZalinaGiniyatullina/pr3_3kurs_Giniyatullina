package com.example.criminalintent

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat
import androidx.core.content.ContentProviderCompat.*
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import java.text.DateFormat
import java.util.Locale

class CrimeFragmentt : Fragment() {
    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var suspectButton: Button
    private lateinit var reportButton: Button

    // для неявного интента выбора контакта из телефонной книги
    private val pickContactLauncher = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        contactUri?.let { uri ->
            val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
            val cursor = requireActivity().contentResolver.query(uri, queryFields, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val suspectName = it.getString(0)
                    crime.suspect = suspectName
                    suspectButton.text = suspectName
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.crime_fragmentt, container, false)
        titleField = view.findViewById(R.id.crime_title)
        dateButton = view.findViewById(R.id.crime_date)
        solvedCheckBox = view.findViewById(R.id.crime_solved)

        // 1. Инициализируем новые кнопки для неявных интентов по ID из вашего макета
        suspectButton = view.findViewById(R.id.crime_suspect)
        reportButton = view.findViewById(R.id.crime_report)

        updateDateButtonText()
        checkAndUpdateButtonState()

        // 2. Логика для кнопки выбора подозреваемого
        suspectButton.setOnClickListener {
            pickContactLauncher.launch(null)
        }

        // 3. Логика неявного интента для отправки текстового отчета в другие приложения
        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            }.also { intent ->
                // Используем ваш ресурс send_report ("Send crime report via") для диалога выбора программ
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                crime.title = sequence.toString()
                checkAndUpdateButtonState()
            }

            override fun afterTextChanged(sequence: Editable?) {}
        }
        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.setOnCheckedChangeListener { _, isChecked ->
            crime.isSolved = isChecked
            checkAndUpdateButtonState()

            val message = if (isChecked)
            {
                "Crime marked as solved!"
            } else
            {
                "Crime marked as unsolved"
            }

            val snackbar = Snackbar.make(
                requireView(),
                message,
                Snackbar.LENGTH_LONG
            )

            snackbar.setAction("CANCEL") { view ->
                crime.isSolved = !isChecked
                solvedCheckBox.isChecked = !isChecked
                checkAndUpdateButtonState()
                Toast.makeText(
                    requireContext(),
                    "Action cancelled",
                    Toast.LENGTH_SHORT
                ).show()
            }

            snackbar.setActionTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
            )
            snackbar.show()
        }

        dateButton.setOnClickListener {
            if (isFormValid())
            {
                showDatePickerDialog()
            } else
            {
                showValidationSnackbar()
            }
        }
    }

    private fun showValidationSnackbar() {
        val message = when {
            crime.title.isBlank() && !crime.isSolved ->
                "Please enter a title and check Solved"
            crime.title.isBlank() ->
                "Please enter a title for the crime"
            else ->
                "Please check the Solved checkbox"
        }
        val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
        snackbar.setActionTextColor(Color.RED)
        snackbar.setAction("OK") {}
        snackbar.show()
    }

    private fun updateDateButtonText() {
        val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        dateButton.text = dateFormat.format(crime.date)
    }

    private fun showDatePickerDialog() {
        Toast.makeText(
            requireContext(),
            "Select date",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun isFormValid(): Boolean {
        return crime.title.isNotBlank() && crime.isSolved
    }

    private fun checkAndUpdateButtonState() {
        dateButton.isEnabled = isFormValid()
    }

    // Вспомогательная функция для генерации динамического текста отчета
    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) "Solved" else "Not Solved"
        val dateString = android.text.format.DateFormat.format("EEE, MMM dd", crime.date).toString()
        val suspectString = if (crime.suspect.isBlank()) "no suspect" else crime.suspect

        return "Office crime: ${crime.title}. Date: $dateString. Status: $solvedString. Suspect: $suspectString."
    }
}