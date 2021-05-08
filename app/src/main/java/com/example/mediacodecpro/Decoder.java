package com.example.mediacodecpro;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by chuibai on 2017/3/10.<br />
 */

public class Decoder {

    public static final int TRY_AGAIN_LATER = -1;
    public static final int BUFFER_OK = 0;
    public static final int BUFFER_TOO_SMALL = 1;
    public static final int OUTPUT_UPDATE = 2;

    private final String MIME_TYPE = "video/avc";
    private MediaCodec mMC = null;
    private MediaFormat mMF;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private long BUFFER_TIMEOUT = 0;
    private MediaCodec.BufferInfo mBI;
    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;

    /**
     * 初始化编码器
     * @throws IOException 创建编码器失败会抛出异常
     */
    public void init() throws IOException {
        mMC = MediaCodec.createDecoderByType(MIME_TYPE);
        mBI = new MediaCodec.BufferInfo();
    }

    /**
     * 配置解码器
     * @param sps 用于配置的sps参数
     * @param pps 用于配置的pps参数
     * @param surface 用于解码显示的Surface
     */
    public void configure(byte[] sps, byte[] pps, Surface surface){
        int[] width = new int[1];
        int[] height = new int[1];
        AvcUtils.parseSPS(sps, width, height);//从sps中解析出视频宽高
        mMF = MediaFormat.createVideoFormat(MIME_TYPE, width[0], height[0]);
        mMF.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
        mMF.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
        mMF.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width[0] * height[0]);
        mMC.configure(mMF, surface, null, 0);
    }

    /**
     * 开启解码器，获取输入输出缓冲区
     */
    public void start(){
        mMC.start();
        mInputBuffers = mMC.getInputBuffers();
        mOutputBuffers = mMC.getOutputBuffers();
    }

    /**
     * 输入数据
     * @param data 输入的数据
     * @param len 数据有效长度
     * @param timestamp 时间戳
     * @return 成功则返回{@link #BUFFER_OK} 否则返回{@link #TRY_AGAIN_LATER}
     */
    public int input(byte[] data,int len,long timestamp){
        int i = mMC.dequeueInputBuffer(BUFFER_TIMEOUT);
        if(i >= 0){
            ByteBuffer inputBuffer = mInputBuffers[i];
            inputBuffer.clear();
            inputBuffer.put(data, 0, len);
            mMC.queueInputBuffer(i, 0, len, timestamp, 0);
        }else {
            return TRY_AGAIN_LATER;
        }
        return BUFFER_OK;
    }

    public int output(byte[] data,int[] len,long[] ts){
        int i = mMC.dequeueOutputBuffer(mBI, BUFFER_TIMEOUT);
        if(i >= 0) {
            if (i < mOutputBuffers.length && mOutputBuffers[i] != null)
            {
                mOutputBuffers[i].position(mBI.offset);
                mOutputBuffers[i].limit(mBI.offset + mBI.size);

                if (data != null)
                    mOutputBuffers[i].get(data, 0, mBI.size);
                len[0] = mBI.size;
                ts[0] = mBI.presentationTimeUs;
            }
            mMC.releaseOutputBuffer(i, true);
        }else{
            return TRY_AGAIN_LATER;
        }
        return BUFFER_OK;
    }

    public void flish(){
        mMC.flush();
    }

    public void release() {
        mMC.stop();
        mMC.release();
        mMC = null;
        outputBuffers = null;
        inputBuffers = null;
    }
}
