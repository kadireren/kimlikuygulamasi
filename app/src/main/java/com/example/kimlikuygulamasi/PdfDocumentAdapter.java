package com.example.kimlikuygulamasi;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PdfDocumentAdapter extends PrintDocumentAdapter {

    private static final String TAG = "PdfDocumentAdapter";
    private final String filePath;

    public PdfDocumentAdapter(Context context, String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
        if (cancellationSignal.isCanceled()) {
            Log.d(TAG, "Yerleşim işlemi iptal edildi");
            callback.onLayoutCancelled();
            return;
        }

        try {
            File file = new File(filePath);
            String fileName = file.getName();

            PrintDocumentInfo.Builder builder = new PrintDocumentInfo.Builder(fileName);
            builder.setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN);

            PrintDocumentInfo info = builder.build();

            callback.onLayoutFinished(info, !newAttributes.equals(oldAttributes));
            Log.d(TAG, "Yerleşim tamamlandı: " + fileName);
        } catch (Exception e) {
            String errorMsg = "Yerleşim oluşturulurken hata: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            callback.onLayoutFailed(errorMsg);
        }
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
        InputStream in = null;
        OutputStream out = null;

        try {
            File file = new File(filePath);
            in = new FileInputStream(file);
            out = new FileOutputStream(destination.getFileDescriptor());

            byte[] buf = new byte[16384];
            int size;
            int totalBytes = 0;

            while ((size = in.read(buf)) >= 0 && !cancellationSignal.isCanceled()) {
                out.write(buf, 0, size);
                totalBytes += size;
            }

            if (cancellationSignal.isCanceled()) {
                Log.d(TAG, "Yazma işlemi iptal edildi");
                callback.onWriteCancelled();
            } else {
                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                Log.d(TAG, "Yazma tamamlandı: " + totalBytes + " byte");
            }

        } catch (Exception e) {
            String errorMsg = "PDF yazdırılırken hata: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            callback.onWriteFailed(errorMsg);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                String errorMsg = "Stream kapatılırken hata: " + e.getMessage();
                Log.e(TAG, errorMsg, e);
            }
        }
    }
}