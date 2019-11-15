package io.carlol.android.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.carlol.android.sample.adapter.TicketRecyclerViewAdapter
import io.carlol.android.sample.modelview.TicketModelView
import kotlinx.android.synthetic.main.activity_horizontal.*

class TicketHorizontalActivity : AppCompatActivity() {

    companion object {

        fun start(context: Context) {
            context.startActivity(Intent(context, TicketHorizontalActivity::class.java))
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horizontal)

        ticketRecyclerView.adapter = TicketRecyclerViewAdapter().apply {
            val sampleText = getString(R.string.sample_text)

            updateDataset(ArrayList<TicketModelView>().apply {
                for (i in 1..30) {
                    add(TicketModelView(R.drawable.ic_qr_code, sampleText))
                }
            })
        }
    }

}
