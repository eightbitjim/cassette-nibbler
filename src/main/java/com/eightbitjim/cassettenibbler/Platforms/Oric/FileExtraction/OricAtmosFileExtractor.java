package com.eightbitjim.cassettenibbler.Platforms.Oric.FileExtraction;

public class OricAtmosFileExtractor extends OricOneFileExtractor {
    public OricAtmosFileExtractor() {
        stateMachine = new OricFileStateMachine(OricTapeFile.FileType.ORIC_ATMOS);
    }
}
