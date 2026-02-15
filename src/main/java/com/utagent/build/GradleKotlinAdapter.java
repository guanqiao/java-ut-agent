package com.utagent.build;

import java.io.File;

public class GradleKotlinAdapter extends GradleAdapter {

    @Override
    public String name() {
        return "gradle-kotlin";
    }

    @Override
    public boolean detect(File projectRoot) {
        return new File(projectRoot, "build.gradle.kts").exists();
    }
}
