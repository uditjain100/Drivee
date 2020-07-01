package udit.programmer.co.drivee

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_register.*
import udit.programmer.co.drivee.Models.Customer

class RegisterActivity : AppCompatActivity() {

    private lateinit var firebaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        firebaseRef = FirebaseDatabase.getInstance().reference.child("Customers")

        register_btn.setOnClickListener {
            if (emptyCheck()) {
                progress_bar.visibility = View.VISIBLE
                val customer = Customer(
                    firstName = firstname_input.text.toString(),
                    lastName = lastname_input.text.toString(),
                    number = mobileNumber_input.text.toString()
                )
                firebaseRef.child(mobileNumber_input.text.toString())
                    .setValue(customer)
                    .addOnCompleteListener {
                        progress_bar.visibility = View.GONE
                        Toast.makeText(this, "Registered Successfully", Toast.LENGTH_LONG).show()
                        Thread.sleep(2000)
                        finish()
                    }.addOnFailureListener {
                        progress_bar.visibility = View.GONE
                        Toast.makeText(this, "FAILED : $it", Toast.LENGTH_LONG).show()
                    }
            }
        }

    }

    private fun emptyCheck(): Boolean {
        progress_bar.visibility = View.VISIBLE
        if (firstname_input.text.isNullOrEmpty()) {
            firstname_input.error = "This field is Empty"
            progress_bar.visibility = View.GONE
            return false
        }
        if (lastname_input.text.isNullOrEmpty()) {
            lastname_input.error = "This field is Empty"
            progress_bar.visibility = View.GONE
            return false
        }
        if (mobileNumber_input.text.isNullOrEmpty()) {
            mobileNumber_input.error = "This field is Empty"
            progress_bar.visibility = View.GONE
            return false
        }
        if (mobileNumber_input.text.toString().length != 10) {
            mobileNumber_input.error = "Enter valid Phone Number"
            progress_bar.visibility = View.GONE
            return false
        }
        return true
    }

}