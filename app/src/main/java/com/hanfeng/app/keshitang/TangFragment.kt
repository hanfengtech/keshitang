package com.hanfeng.app.keshitang

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import com.hanfeng.app.keshitang.databinding.FragmentTangBinding
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import java.lang.Exception

/**
 * A simple [Fragment] subclass.
 */
class TangFragment : Fragment() {

    private var mRemoteContainer: ConstraintLayout? = null
    private var mRemoteView: SurfaceView? = null
    private var mLocalContainer: FrameLayout? = null
    private var mLocalView: SurfaceView? = null
    private var mBinding: FragmentTangBinding? = null
    private var mRtcEngine: RtcEngine? = null
    private var mMuteButton: ImageView? = null
    private var mCallButton: ImageView? = null
    private var mSwitchButton: ImageView? = null
    private var mAgentButton: ImageView? = null
    private var mIsCalling = false
    private var mIsMuted = false
    private var mIsSwitched = false
    private var mIsHost = true

    private var mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            Log.d("TangFragment", "Join $channel channel successfully!")
        }

        override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteAudioStateChanged(uid, state, reason, elapsed)
            Log.d("TangFragment", "State: $state")
            if (state == Constants.REMOTE_VIDEO_STATE_STARTING) {
                activity?.runOnUiThread{
                    Log.d("TangFragment", "SETUP Remote View!")
                    setUpRemoteView(uid)
                }
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            Log.d("TangFragment", "NEW user just joined!  " + uid)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            Log.d("TangFragment", "User just left!!  " + uid)
            activity?.runOnUiThread{ removeRemoteView() }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mBinding = FragmentTangBinding.inflate(inflater)
        initUI()

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) && checkSelfPermission(
                REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID)) {
            initEngineAndJoinChannel()
        }

        return mBinding?.root
    }

    private fun initUI() {
        mRemoteContainer = mBinding?.remoteVideoViewContainer
        mLocalContainer = mBinding?.localVideoViewContainer
        mMuteButton = mBinding?.muteButton
        mMuteButton?.setOnClickListener {
            onMuteClicked(it)
        }
        mCallButton = mBinding?.callButton
        mCallButton?.setOnClickListener {
            onCallClicked(it)
        }
        mSwitchButton = mBinding?.switchCameraButton
        mSwitchButton?.setOnClickListener {
            onSwitchCameraClicked(it)
        }
        mAgentButton = mBinding?.agentButton
        mAgentButton?.setOnClickListener {
            onAgentChangeClicked(it)
        }
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this.requireContext(), permission)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this.requireActivity(), REQUESTED_PERMISSIONS,
                                              requestCode)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQ_ID -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED
                    || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    Log.d("TangFragment", "Permission not granted!!!")
                } else {
                    initEngineAndJoinChannel()
                }
            }
        }
    }

    private fun initEngineAndJoinChannel() {
        initializeEngine()
    }

    private fun removeRemoteView() {
        if (mRemoteView != null) {
            mRemoteContainer?.removeView(mRemoteView)
        }
        mRemoteView = null
    }

    private fun setUpRemoteView(uid: Int) {
        mRemoteView = RtcEngine.CreateRendererView(activity!!.baseContext)
        mRemoteContainer?.addView(mRemoteView)
        val remoteVideoCanvas = VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        mRtcEngine?.setupRemoteVideo(remoteVideoCanvas)

        mMuteButton?.bringToFront()
        mCallButton?.bringToFront()
        mSwitchButton?.bringToFront()
        mAgentButton?.bringToFront()
        mLocalView?.bringToFront()
    }

    private fun initializeEngine() {
        try {
            mRtcEngine = RtcEngine.create(activity!!.baseContext, getString(R.string.agora_app_id),
                                          mRtcEventHandler)
            mRtcEngine?.enableWebSdkInteroperability(true)
            mRtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TangFragment", "NOT ABLE TO GET ENGINE!!!!!!!!")
        }
    }

    private fun setUpLocalView() {
        mRtcEngine?.enableVideo()
        mLocalView = RtcEngine.CreateRendererView(activity!!.baseContext)
        mLocalContainer?.addView(mLocalView)
        mLocalView?.setZOrderMediaOverlay(true)

        val videoCanvas = VideoCanvas(mLocalView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        mRtcEngine?.setupLocalVideo(videoCanvas)
    }

    private fun joinChannel() {
        mRtcEngine!!.joinChannel(null, "android", "", 0)
        mIsCalling = true
    }

    override fun onDestroy() {
        super.onDestroy()
        leaveChannel()
        RtcEngine.destroy()
    }

    private fun leaveChannel() {
        mRtcEngine!!.leaveChannel()
        Log.i("TangFragment", "Leave channel")
        mIsCalling = false
    }

    private fun onAgentChangeClicked(view: View) {
        mIsHost = !mIsHost
        setUpdateAgent()
    }

    private fun setUpdateAgent() {
        Log.i("TangFragment", "Host? " + mIsHost)
        if (mIsHost) {
            mRtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            mLocalView?.setZOrderMediaOverlay(true)
            mLocalView?.visibility = View.VISIBLE
        } else {
            mRtcEngine?.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)
            mLocalView?.setZOrderMediaOverlay(false)
            mLocalView?.visibility = View.INVISIBLE
        }
        mAgentButton?.setImageResource(if (mIsHost) R.drawable.host else R.drawable.client)
    }

    private fun onMuteClicked(view: View) {
        mIsMuted = !mIsMuted
        mRtcEngine?.muteLocalAudioStream(mIsMuted)
        Log.i("TangFragment", "Muted? " + mIsMuted)
        mMuteButton?.setImageResource(if (!mIsMuted) R.drawable.btn_mute_normal else R.drawable.btn_mute_pressed)
    }

    private fun endCall() {
        leaveChannel()
        removeRemoteView()
        removeLocalView()
    }

    private fun removeLocalView() {
        mLocalContainer?.removeView(mLocalView)
        mLocalView = null
    }

    private fun makeCall() {
        setUpLocalView()
        setUpdateAgent()
        joinChannel()
    }

    private fun onCallClicked(view: View) {
        Log.i("TangFragment", "Is calling? " + mIsCalling)
        mCallButton?.setImageResource(if (mIsCalling) R.drawable.btn_startcall_pressed else R.drawable.btn_endcall_pressed)
        if (mIsCalling) {
            endCall()
        } else {
            makeCall()
        }
    }

    private fun onSwitchCameraClicked(view: View) {
        mIsSwitched = !mIsSwitched
        mRtcEngine?.switchCamera()
        Log.i("TangFragment", "Using " + if (mIsSwitched) "back " else "front " + "camera")
        mSwitchButton?.setImageResource(if (mIsSwitched) R.drawable.btn_switch_camera_normal else R.drawable.btn_switch_camera_pressed)
    }

    companion object {
        private const val PERMISSION_REQ_ID = 22
        private val REQUESTED_PERMISSIONS =
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    }
}
