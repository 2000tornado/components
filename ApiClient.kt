
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.net.ConnectivityManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class ApiClient<T> {
    lateinit var activity: Activity
    

    fun callApi(activity: Activity, root: View, apiCall: Call<T>, dialog: Dialog? = null, serviceGenerator: (T) -> Unit) {
        this.activity = activity
        
        if (hasInternetConnect()) {
            dismissNoNet()
            if (dialog == null) showProgress()
            apiCall.clone().enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    if (dialog == null) dismissProgress()

                    if (response.code() == 200 && response.body() != null) {
                        serviceGenerator(response.body()!!)
                    } else {
                        try {
                            val jsonObject = JSONObject(response.errorBody()!!.string())
                            errorOrMsg = jsonObject.getString("text")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        Snackbar.make(root, "Something went wrong", Snackbar.LENGTH_LONG).setAction("dismiss", null).show()
                    }
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    if (dialog == null) dismissProgress() else dialog.dismiss()
                    t.printStackTrace()
                    Snackbar.make(root, "${if (t is SocketTimeoutException) "Time out" else t.message}", Snackbar.LENGTH_INDEFINITE)
                            .setAction(
                            if (t is SocketTimeoutException) "Try again" else "dismiss",
                            if (t is SocketTimeoutException) View.OnClickListener { callApi(activity, root, apiCall, dialog, serviceGenerator) } else null
                    ).show()
                }
            })
        } else {
            showNoNet()
            retryClick.setOnClickListener {
                callApi(activity, root, apiCall, dialog, serviceGenerator)
            }
        }
    }

    private lateinit var noNet: Dialog
    lateinit var retryClick: View
    private fun initNoNetDialog() {
        noNet = Dialog(activity)
        val noNetBind = DialogNoNetBinding.inflate(activity.layoutInflater)
        noNet.setContentView(noNetBind.root)
        noNet.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        noNet.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        noNet.setCancelable(false)
        retryClick = noNetBind.root

    }

    private fun showNoNet() {
        if (!this::noNet.isInitialized) initNoNetDialog()
        noNet.show()
    }

    private fun dismissNoNet() {
        if (this::noNet.isInitialized) noNet.dismiss()
    }

    lateinit var progress: Dialog
    private fun initProgressDialog() {
        progress = Dialog(activity)
        progress.setContentView(DialogProgressBinding.inflate(activity.layoutInflater).root)
        progress.window!!.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
        progress.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        progress.setCancelable(false)
    }

    private fun showProgress() {
        if (!this::progress.isInitialized) initProgressDialog()
        if (!progress.isShowing) progress.show()
    }

    private fun dismissProgress() {
        if (this::progress.isInitialized) progress.dismiss()
    }

    fun hasInternetConnect(): Boolean {
        var isWifiConnected = false
        var isMobileConnected = false
        val netInfo = (activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).allNetworkInfo
        for (ni in netInfo) {
            if (ni.typeName.equals("WIFI", ignoreCase = true)) if (ni.isConnected) isWifiConnected = true
            if (ni.typeName.equals("MOBILE", ignoreCase = true)) if (ni.isConnected) isMobileConnected = true
        }
        return isWifiConnected || isMobileConnected
    }
}

val api: Api
    get() {
        val okClient = OkHttpClient.Builder().addInterceptor { chain: Interceptor.Chain ->
            val request = chain.request()
            chain.proceed(request.newBuilder().addHeader("Cache-Control", "no-cache").method(request.method, request.body).build())

        }.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))      //todo remove this line in production
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        return Retrofit.Builder()
                .baseUrl("https://pixabay.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okClient)
                .build()
                .create(Api::class.java)
    }

/**test adapter*/
/*class MyAdapter(var activity: Activity, var list: ArrayList<Any>, val click: (Any) -> Unit) : RecyclerView.Adapter<MyAdapter.VH>() {
    override fun onBindViewHolder(holder: VH, pos: Int) {
        val bind = holder.binding
        val data = list[pos]


        bind.root.setOnClickListener {
            click(data)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(RvMovieCatBinding.inflate(LayoutInflater.from(parent.context)))
    class VH(var binding: RvMovieCatBinding) : RecyclerView.ViewHolder(binding.root)
    override fun getItemCount() = list.size
}*/

/**no net xml**/
/*<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/transparent"
    android:orientation="vertical">

    <RelativeLayout
        android:id="@+id/rlNoNet"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#E3E3E3"
        android:elevation="@dimen/_10sdp"
        android:gravity="center">

        <ImageView
            android:id="@+id/ivNoNet"
            android:layout_width="@dimen/_192sdp"
            android:layout_height="@dimen/_192sdp"
            android:layout_centerHorizontal="true"
            android:scaleType="fitCenter"
            android:src="@drawable/ic_no_net" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_below="@id/ivNoNet"
            android:layout_centerHorizontal="true"
            android:layout_marginTop="@dimen/_14sdp"
            android:gravity="center"
            android:letterSpacing=".1"
            android:text="No internet connection\nTap to Retry"
            android:textColor="#506470"
            android:textSize="@dimen/_16sdp" />
    </RelativeLayout>
</RelativeLayout>*/


/** progress xml **/
/*<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_gravity="center"
    android:gravity="center"
    android:orientation="vertical">

    <ProgressBar
        android:id="@+id/pb"
        style="?android:attr/progressBarStyleLarge"
        android:layout_width="@dimen/_35sdp"
        android:layout_height="@dimen/_35sdp"
        android:indeterminateTint="@color/colorAccent" />
</LinearLayout>*/
