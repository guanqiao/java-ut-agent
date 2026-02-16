package com.utagent.maintenance;

import java.io.File;

public record FileChange(
    File file,
    String oldContent,
    String newContent
) {}
