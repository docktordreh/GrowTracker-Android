package me.anon.grow.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import me.anon.grow.R
import me.anon.lib.TempUnit
import me.anon.lib.ext.formatWhole
import me.anon.model.TemperatureChange
import java.util.*

class TemperatureDialogFragment(var action: TemperatureChange? = null, val callback: (action: TemperatureChange) -> Unit = {}) : DialogFragment()
{
	private val newAction = action == null

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
	{
		val tempUnit = TempUnit.getSelectedTemperatureUnit(requireContext())
		val view = LayoutInflater.from(activity).inflate(R.layout.temperature_dialog, null)
		val dialog = AlertDialog.Builder(requireContext())
		dialog.setTitle(R.string.temperature_title)
		dialog.setPositiveButton(if (newAction) R.string.add else R.string.edit) { dialog, which ->
			if (view.findViewById<EditText>(R.id.temperature_input).text.toString().isNotBlank())
			{
				action?.let {
					it.temp = tempUnit.to(TempUnit.CELCIUS, view.findViewById<EditText>(R.id.temperature_input).text.toString().toDouble())

					if (view.findViewById<EditText>(R.id.notes).text.isNotEmpty())
					{
						it.notes = view.findViewById<EditText>(R.id.notes).text.toString()
					}

					callback(it)
				}
			}
		}

		if (action == null)
		{
			action = TemperatureChange()
		}

		view.findViewById<EditText>(R.id.notes).setText(action?.notes)
		view.findViewById<EditText>(R.id.temperature_input).hint = "32°" + tempUnit.label
		if (!newAction)
		{
			view.findViewById<EditText>(R.id.temperature_input).setText("${TempUnit.CELCIUS.to(tempUnit, action!!.temp).formatWhole()}")
		}

		val date = Calendar.getInstance()
		date.timeInMillis = action!!.date

		val dateFormat = android.text.format.DateFormat.getDateFormat(activity)
		val timeFormat = android.text.format.DateFormat.getTimeFormat(activity)

		val dateStr = dateFormat.format(Date(action!!.date)) + " " + timeFormat.format(Date(action!!.date))

		view.findViewById<TextView>(R.id.date).text = dateStr
		view.findViewById<TextView>(R.id.date).setOnClickListener {
			val fragment = DateDialogFragment.newInstance(action!!.date)
			fragment.setOnDateSelected(object : DateDialogFragment.OnDateSelectedListener
			{
				override fun onDateSelected(date: Calendar)
				{
					val dateStr = dateFormat.format(date.time) + " " + timeFormat.format(date.time)
					view.findViewById<TextView>(R.id.date).text = dateStr

					action!!.date = date.timeInMillis
					onCancelled()
				}

				override fun onCancelled()
				{
					childFragmentManager.beginTransaction().remove(fragment).commit()
				}
			})
			childFragmentManager.beginTransaction().add(fragment, "date").commit()
		}

		dialog.setView(view)
		dialog.setNegativeButton(R.string.cancel) { dialogInterface, i -> onCancel(dialogInterface) }

		return dialog.create()
	}
}
