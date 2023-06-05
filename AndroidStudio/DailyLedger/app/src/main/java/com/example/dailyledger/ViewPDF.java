package com.example.dailyledger;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;

public class ViewPDF extends AppCompatActivity {
    ImageView imgView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pdf);

        imgView = findViewById(R.id.imgView);
        Intent i = getIntent();
        String filepath = i.getStringExtra("filePath");
        File file = new File(filepath);

        try {
            PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));

            // Get the total number of pages in the PDF
            int pageCount = renderer.getPageCount();

            // Display the first page of the PDF
            if (pageCount > 0) {
                PdfRenderer.Page page = renderer.openPage(0);
                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                // Display the rendered bitmap on an ImageView or any other view of your choice
                imgView.setImageBitmap(bitmap);
                page.close();
            }

            renderer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}