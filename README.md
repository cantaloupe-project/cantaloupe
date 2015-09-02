# Cantaloupe

IIIF 2.0 image server in Java

## Configuration

Create a file called `cantaloupe.properties` anywhere on disk, containing
something like the following:

```
http.port = 8182
image.processor = ImageMagickProcessor
```

## Running

Run it like:

`java -jar Cantaloupe-x.x.x.jar -Dcantaloupe.config=/path/to/cantaloupe.properties`

# Contributing

1. Fork it (https://github.com/medusa-project/cantaloupe/fork)
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create a new Pull Request
