package com.tphelps.backend.controller.pojos;

import java.util.List;

public record StudyGuide(String title, List<Steps> questions) {
}
