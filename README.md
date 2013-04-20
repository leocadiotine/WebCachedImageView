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

The `WebCachedImageView`'s cache is a [LRU cache](https://en.wikipedia.org/wiki/Cache_algorithms#Least_Recently_Used), which means that the images that were least recently used will be the first ones to be purged.
