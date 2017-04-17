# cassette-nibbler
A toolkit to recover data from computer audio cassette tapes from the 1980s. Lots of information on the [wiki](https://github.com/eightbitjim/cassette-nibbler/wiki).

* Use from the command line or integrate into your own tools
* Various options to tolerate errors, e.g. invalid checksums or partially complete files
* Output raw binary data, or files suitable for an emulator, or ASCII text (e.g. de-tokenised BASIC program listing)
* Output partially processed data to allow manual fixing, then process the fixed version

This is *not* an emulator. It is a data recovery tool that I wrote to get back some programs that I created over 30 years ago, which were stored on degraded and damaged audio cassettes. If you have good quality cassettes that you want to load into an emulator, this probably isn't what you're looking for.

This is still in its early stages, and is very much work in progress. It should expand over time and become more reliable as I work my way through more test data. I thought it would share it at this point in case it's useful to anyone.

# supported formats
Currently the following formats are supported to some extent, some reasonably well and others less so:

* **Commodore 64, 128, Vic 20, PET, C16, Plus 4:** the ROM based loaders
* **Commodore 64 Turbo Tape:** the popular turbo loader for the Commodore 64
* **Sinclair ZX Spectrum**
* **Sinclair ZX81**
* **Acorn BBC Micro / Electron** 
* **Tangerine Systems Oric 1 / Atmos** 
* **MSX compatibles**
* **Amstrad CPC**
* **Dragon 32**
* **TRS-80** (untested)
* **Apple II** (untested)
* **(experimental) c64 Turbo Loaders:** tries automatically to extract from any C64 turbo loader, making some guesses along the way. The results are variable.
* **(experimental) Automatic extraction:** tries automatically to identify the encoding scheme and extract raw binary data. Sometimes it works.

More information on the [wiki](https://github.com/eightbitjim/cassette-nibbler/wiki)

You should try to provide good quality audio files as input. Ideally, uncompressed WAV files of 44100 Hz. If you have heavily compressed files, e.g. mp3, then results may be disappointing.

# build

See the [wiki](https://github.com/eightbitjim/cassette-nibbler/wiki) for build instructions, examples and more information.
