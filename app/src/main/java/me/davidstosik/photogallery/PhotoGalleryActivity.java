package me.davidstosik.photogallery;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    @Override
    public Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }
}
