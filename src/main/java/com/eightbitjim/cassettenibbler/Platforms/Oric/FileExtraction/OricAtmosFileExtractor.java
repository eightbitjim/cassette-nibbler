package com.eightbitjim.cassettenibbler.Platforms.Oric.FileExtraction;

public class OricAtmosFileExtractor extends OricOneFileExtractor {
    public OricAtmosFileExtractor(String channelName) {
        super(channelName);
        stateMachine = new OricFileStateMachine(OricTapeFile.FileType.ORIC_ATMOS, channelName);
    }
}
