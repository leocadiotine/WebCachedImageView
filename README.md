#WebCachedImageView

![WebCachedImageView logo](https://dl.dropboxusercontent.com/u/5135185/blog/wciv.png)

An `ImageView` capable of downloading images from the web and caching them for faster and offline access.

##Purpose
Mobile devices have limited processor power, battery and data connectivity. Hence, it's never a good idea to download images over and over again when they need to be displayed.

A simple but common reproduction of this problem is when you have a `ListView` with images. Since Android's `Adapter` recycles the list's views when the user scrolls, the images have to be downloaded and re-downloaded for each scroll action.

`WebCachedImageView` solves this problem by downloading the image and caching it on memory and on disk. So subsequent displays of the same image don't need to download data nor require a lot of processor power. 

##How it works
`WebCachedImageView` downloads the image on a background thread, without blocking the UI. After the image is downloaded for the first time, it is cached on memory and on disk, according to the current configuration.

When the image needs to be loaded again, `WebCachedImageView` will first look for it on memory, because it's faster. If the image was garbage collected, it will look for it on disk.

##Pro features

###Caching
The `WebCachedImageView`'s cache is a [LRU cache](https://en.wikipedia.org/wiki/Cache_algorithms#Least_Recently_Used), which means that the images that were least recently used will be the first ones to be purged.

###Concurrency
`WebCachedImageView` handles concurrency, so you can use it inside `AdapterView`s (like `ListView`) and recycle it without worrying about mismatched threads.

###Image sampling according to the view size
`WebCachedImageView` will download and cache only *the amount of pixels that actually need to be displayed*. It means that if you have a `WebCachedImageView` of `100dp x 100dp` on a `hdpi` screen, and have to download a `500px x 500px` image, the library will sample the image, resize it and only download a subset of `150px x 150px`.  

##Usage
You can find a whole working example on the `DumbledroidExample` folder of the [Dumbledroid](https://github.com/leocadiotine/Dumbledroid) repository. It's a working Android project that uses `WebCachedImageView` to download the images. You can see the correct usage at [`flickr_result_row.xml`](https://github.com/leocadiotine/Dumbledroid/blob/master/DumbledroidExample/res/layout/flickr_result_row.xml) and [`FlickrAdapter.java`](https://github.com/leocadiotine/Dumbledroid/blob/master/DumbledroidExample/src/io/leocad/dumbledoreexample/adapters/FlickrAdapter.java).

But for the purposes of this manual, following are step by step instructions.

###Step 1: Add the library to your Android project
Import the `WebCachedImageView` project on Eclipse and [reference it as a library project](https://developer.android.com/tools/projects/projects-eclipse.html#ReferencingLibraryProject) of your main project.

###Step 2: Add the permissions to your AndroidManifest.xml
`WebCachedImageView` requires the following permissions to work:

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

The `WRITE_EXTERNAL_STORAGE` is only required if you enable the disk cache.

###Step 3: Add the view to your XML layout
You need to add the `WebCachedImageView` tag to your XML layout file:

    <WebCachedImageView
        android:id="@+id/wciv"
        android:layout_width="90dp"
        android:layout_height="90dp" />

###Step 4 (optional): Fine-tuning
If you want to, you can fine-tune `WebCachedImageView`'s settings. To do that, first you'll have to add the following attribute to your root XML node:

    xmlns:app="http://schemas.android.com/res-auto"

####Memory allocation
By default, `WebCachedImageView` allocates 1/8 (12.5%) of the phone's memory to the memory cache. To change that, set a `float` value to the `app:memoryPercentToUse` attribute.

####Caching mode
`WebCachedImageView` can cache in two levels: memory and disk. By default' it uses both. But you can set the `app:mode` value to `no_cache`, `memory` or `disk`.

When it's all set, your XML will look something like this:

    <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/res-auto"
        android:layout_width="match_parent"
        android:layout_height="match_parent" >

        <WebCachedImageView
            android:id="@+id/wciv"
            android:layout_width="90dp"
            android:layout_height="90dp"
            app:memoryPercentToUse="20.0"
            app:mode="memory" />
            
    </RelativeLayout>
        
###Step 5: Download the image
Just call the following code whenever you need to download an image:

    WebCachedImageView wciv = (WebCachedImageView) findViewById(R.id.wciv);
    wciv.setImageUrl(YOUR_IMAGE_URL);
    
You don't have to spawn a new thread or create an `AsyncTask` to do that. `WebCachedImageView` does that for you.

##License
WebCachedImageView source code is released under BSD 2-clause license. Check LICENSE file for more information.

If you use this code, I'd appreciate you refer my name (Leocadio Tiné) and the link to this project's page in your project's website or credits screen. Though you don't have any legal/contractual obligation to do so, just good karma.

##Suggestions? Comments?
Pull requests are always welcome. So are donations :)

To find me, buzz at `me[at]leocad.io` or [follow me on Twitter](http://www.twitter.com/leocadiotine). To read interesting stuff, go to [my blog](http://blog.leocad.io).

~~~~
:::::::::::::
::         ::
:: Made at ::
::         ::
:::::::::::::
     ::
Hacker School
:::::::::::::
~~~~

<form action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
<input type="hidden" name="cmd" value="_s-xclick">
<input type="hidden" name="encrypted" value="-----BEGIN PKCS7-----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-----END PKCS7-----
">
<input type="image" src="https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif" border="0" name="submit" alt="PayPal - The safer, easier way to pay online!">
<img alt="" border="0" src="https://www.paypalobjects.com/pt_BR/i/scr/pixel.gif" width="1" height="1">
</form>
