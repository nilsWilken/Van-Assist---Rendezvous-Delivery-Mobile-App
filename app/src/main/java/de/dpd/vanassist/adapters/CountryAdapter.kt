package de.dpd.vanassist.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import de.dpd.vanassist.R
import de.dpd.vanassist.util.language.CountryItem
import kotlinx.android.synthetic.main.country_spinner_row.view.*


class CountryAdapter(context: Context,var items : List<CountryItem>): ArrayAdapter<CountryItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    private fun initView(position:Int, convertView:View?, parent:ViewGroup):View {
        var myView = convertView
        if(myView == null) {
            myView = LayoutInflater.from(context).inflate(R.layout.country_spinner_row, parent, false)
        }
        val imageViewFlag = myView!!.country_spinner as ImageView

        val currentItem: CountryItem? = getItem(position)
        if(currentItem != null) {
            imageViewFlag.setImageResource(currentItem.imageFlag)
        }

        return myView
    }

}