package com.example.mediacodecpro;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by chuibai on 2017/3/10.<br />
 */

public class Encoder {

    public static final int TRY_AGAIN_LATER = -1;
    public static final int BUFFER_OK = 0;
    public static final int BUFFER_TOO_SMALL = 1;
    public static final int OUTPUT_UPDATE = 2;

    private int format = 0;
    private final String MIME_TYPE = "video/avc";
    private MediaCodec mMC = null;
    private MediaFormat mMF;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private long BUFFER_TIMEOUT = 0;
    private MediaCodec.BufferInfo mBI;

    /**
     * 初始化编码器
     * @throws IOException 创建编码器失败会抛出异常
     */
    public void init() throws IOException {
        mMC = MediaCodec.createEncoderByType(MIME_TYPE);
        format = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
        mBI = new MediaCodec.BufferInfo();
    }

    /**
     * 配置编码器，需要配置颜色、帧率、比特率以及视频宽高
     * @param width 视频的宽
     * @param height 视频的高
     * @param bitrate 视频比特率
     * @param framerate 视频帧率
     */
    public void configure(int width,int height,int bitrate,int framerate){
        if(mMF == null){
            mMF = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
            mMF.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            mMF.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
            if (format != 0){
                mMF.setInteger(MediaFormat.KEY_COLOR_FORMAT, format);
            }
            mMF.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, -1); //关键帧间隔时间 单位s
        }
        mMC.configure(mMF,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    /**
     * 开启编码器，获取输入输出缓冲区
     */
    public void start(){
        mMC.start();
        inputBuffers = mMC.getInputBuffers();
        outputBuffers = mMC.getOutputBuffers();
    }

    /**
     * 向编码器输入数据，此处要求输入YUV420P的数据
     * @param data YUV数据
     * @param len 数据长度
     * @param timestamp 时间戳
     * @return
     */
    public int input(byte[] data,int len,long timestamp){
        int index = mMC.dequeueInputBuffer(BUFFER_TIMEOUT);
        Log.e("...","" + index);
        if(index >= 0){
            ByteBuffer inputBuffer = inputBuffers[index];
            inputBuffer.clear();
            if(inputBuffer.capacity() < len){
                mMC.queueInputBuffer(index, 0, 0, timestamp, 0);
                return BUFFER_TOO_SMALL;
            }
            inputBuffer.put(data,0,len);
            mMC.queueInputBuffer(index,0,len,timestamp,0);
        }else{
            return index;
        }
        return BUFFER_OK;
    }

    /**
     * 输出编码后的数据
     * @param data 数据
     * @param len 有效数据长度
     * @param ts 时间戳
     * @return
     */
    public int output(/*out*/byte[] data,/* out */int[] len,/* out */long[] ts){
        int i = mMC.dequeueOutputBuffer(mBI, BUFFER_TIMEOUT);
        if(i >= 0){
            if(mBI.size > data.length) return BUFFER_TOO_SMALL;
            outputBuffers[i].position(mBI.offset);
            outputBuffers[i].limit(mBI.offset + mBI.size);
            outputBuffers[i].get(data, 0, mBI.size);
            len[0] = mBI.size ;
            ts[0] = mBI.presentationTimeUs;
            mMC.releaseOutputBuffer(i, false);
        } else if (i == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            outputBuffers = mMC.getOutputBuffers();
            return OUTPUT_UPDATE;
        } else if (i == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            mMF = mMC.getOutputFormat();
            return OUTPUT_UPDATE;
        } else if (i == MediaCodec.INFO_TRY_AGAIN_LATER) {
            return TRY_AGAIN_LATER;
        }

        return BUFFER_OK;
    }

    public void release(){
        mMC.stop();
        mMC.release();
        mMC = null;
        outputBuffers = null;
        inputBuffers = null;
    }

    public void flush() {
        mMC.flush();
    }
}
