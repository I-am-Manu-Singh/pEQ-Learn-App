#!/usr/bin/env python3
"""
Test Engine and Formats Runner for pEQ Learn
Tests:
1. Piano fingering & biomechanics (Scales, Arpeggios, Chords, Hanon across formats)
2. Guitar fretboard positioning & fingering (Open chords, Barre chords, Scale boxes, Drop D)
3. Score file parsing integrity (MIDI, MusicXML, GuitarPro, MXL)
"""

import sys
import os
import math
import struct
import zipfile
import xml.etree.ElementTree as ET

# ==============================================================================
# 1. PIANO BIOMECHANICAL ENGINE (Python mirror of BiomechanicalPerformersEngine)
# ==============================================================================

RH_FINGER_REST = [0.0, -3.6, -1.8, 0.0, 1.8, 3.6]
LH_FINGER_REST = [0.0, 3.6, 1.8, 0.0, -1.8, -3.6]
FINGER_WEIGHTS = [0.0, 1.05, 0.90, 0.88, 1.12, 1.25]
BLACK_KEYS = {1, 3, 6, 8, 10}

def is_black(midi):
    return (midi % 12) in BLACK_KEYS

def solve_single_note_finger(pitch, is_rh, last_palm, last_finger, last_pitch, lookahead=None):
    if lookahead is None:
        lookahead = []
    delta = pitch - last_pitch
    if delta == 0 and 1 <= last_finger <= 5:
        return last_finger

    rest_offsets = RH_FINGER_REST if is_rh else LH_FINGER_REST
    black = is_black(pitch)

    run_length = 0
    curr = pitch
    for np in lookahead:
        if (delta > 0 and np > curr) or (delta < 0 and np < curr):
            run_length += 1
            curr = np
        else:
            break

    best_finger = 3
    best_cost = float('inf')

    for f in range(1, 6):
        cost = 0.0

        f_delta = (f - last_finger) if is_rh else (last_finger - f)
        allow_crossover = (run_length >= 1)

        is_thumb_under = (delta > 0 and f_delta < 0 and allow_crossover and
                          ((is_rh and f == 1 and 2 <= last_finger <= 4 and not black) or
                           (not is_rh and last_finger == 1 and 2 <= f <= 4)))
        is_finger_over = (delta < 0 and f_delta > 0 and allow_crossover and
                          ((is_rh and last_finger == 1 and 2 <= f <= 4) or
                           (not is_rh and f == 1 and 2 <= last_finger <= 4 and not black)))

        # 1. Distance from resting position or crossover pivot point
        if is_thumb_under or is_finger_over:
            cost += abs(delta) * 1.5
        else:
            finger_rest = last_palm + rest_offsets[f]
            cost += abs(pitch - finger_rest) * 2.2 * FINGER_WEIGHTS[f]

        # 2. Same finger on different pitch penalty
        if f == last_finger and delta != 0:
            cost += 14.0 if abs(delta) > 7 else 65.0

        # 3. Directional movement & crossovers
        if delta > 0:
            if f_delta > 0:
                expected_semis = f_delta * 2.0
                cost += abs(delta - expected_semis) * 2.4
            elif f_delta < 0:
                is_standard_pivot = (is_rh and last_finger in (3, 4) and f == 1) or \
                                    (not is_rh and last_finger == 1 and f in (3, 4))
                pivot_cost = 2.0 if is_standard_pivot else 12.0
                cost += pivot_cost if (is_thumb_under and delta <= 4) else 48.0
        elif delta < 0:
            if f_delta < 0:
                expected_semis = (-f_delta) * 2.0
                cost += abs((-delta) - expected_semis) * 2.4
            elif f_delta > 0:
                is_standard_pivot = (is_rh and last_finger == 1 and f in (3, 4)) or \
                                    (not is_rh and last_finger in (3, 4) and f == 1)
                pivot_cost = 2.0 if is_standard_pivot else 12.0
                cost += pivot_cost if (is_finger_over and (-delta) <= 4) else 48.0

        # 4. Black key penalty
        if black:
            if f == 1:
                cost += 22.0
            elif f == 5:
                cost += 8.0

        # 5. Lookahead runway
        if delta > 0:
            available_runway = (5 - f) if is_rh else (f - 1)
            if available_runway == 0 and run_length > 0:
                cost += run_length * 16.0
            elif is_rh and f == 1 and last_finger in (3, 4) and run_length >= 2:
                cost -= 6.0
            elif not is_rh and last_finger == 1 and f in (3, 4) and run_length >= 2:
                cost -= 6.0
        elif delta < 0:
            available_runway = (f - 1) if is_rh else (5 - f)
            if available_runway == 0 and run_length > 0:
                cost += run_length * 16.0
            elif not is_rh and f == 1 and last_finger in (3, 4) and run_length >= 2:
                cost -= 6.0
            elif is_rh and last_finger == 1 and f in (3, 4) and run_length >= 2:
                cost -= 6.0

        if cost < best_cost:
            best_cost = cost
            best_finger = f

    return best_finger

def simulate_piano_sequence(pitches, is_rh, initial_palm=None, initial_finger=None):
    if not pitches:
        return []
    rest_offsets = RH_FINGER_REST if is_rh else LH_FINGER_REST
    palm = initial_palm if initial_palm is not None else (pitches[0] - rest_offsets[1 if is_rh else 5])
    last_finger = initial_finger if initial_finger is not None else (1 if is_rh else 5)
    last_pitch = pitches[0]

    result = []
    for i, p in enumerate(pitches):
        lookahead = pitches[i+1:i+6]
        if i == 0 and initial_finger is not None:
            f = initial_finger
        else:
            f = solve_single_note_finger(p, is_rh, palm, last_finger, last_pitch, lookahead)
        result.append(f)
        palm = max(21, min(108, p - rest_offsets[f]))
        last_finger = f
        last_pitch = p

    return result

# ==============================================================================
# 2. GUITAR BIOMECHANICAL ENGINE (Python mirror of BiomechanicalPerformersEngine)
# ==============================================================================

def solve_fretted_slice(pitches, open_strings, max_fret=24, min_fret=0, last_box_center=3.0, prefer_open=True):
    unique_pitches = sorted(list(set(pitches)))
    if not unique_pitches or not open_strings:
        return {}, last_box_center

    num_strings = len(open_strings)
    lowest_open = open_strings[-1]

    transposed_to_raw = {}
    for p in unique_pitches:
        tp = p
        while tp < lowest_open:
            tp += 12
        transposed_to_raw.setdefault(tp, []).append(p)

    unique_transposed = sorted(transposed_to_raw.keys())[:num_strings]

    best_map = {}
    best_cost = float('inf')
    best_box_center = last_box_center

    max_box_start = max(1, max_fret - 3)

    for box_start in range(max(1, min_fret), max_box_start + 1):
        box_end = box_start + 3
        box_center = box_start + 1.5

        used_strings = [False] * num_strings
        temp_placements = {}

        cost = abs(box_center - last_box_center) * 4.2 + \
               ((box_start - 1) * 1.8 if box_start <= 7 else 12.0 + (box_start - 7) * 4.5)

        for midi in unique_transposed:
            best_str = -1
            best_fret = -1
            best_note_cost = float('inf')

            for s_idx in range(num_strings):
                fret = midi - open_strings[s_idx]
                if fret < min_fret or fret > max_fret:
                    continue

                string_used = used_strings[s_idx]
                if fret == 0:
                    stretch_penalty = 1.5 if (prefer_open and box_start <= 5) else 22.0
                elif box_start <= fret <= box_end:
                    stretch_penalty = (fret - box_start) * 1.2
                elif fret < box_start:
                    stretch_penalty = (box_start - fret) * 38.0
                else:
                    stretch_penalty = (fret - box_end) * 38.0

                wound_penalty = 36.0 if (s_idx >= num_strings - 2 and fret > 8) else (18.0 if (s_idx == num_strings - 3 and fret > 10) else 0.0)
                collision_penalty = 250.0 if string_used else 0.0

                note_cost = stretch_penalty + wound_penalty + collision_penalty
                if note_cost < best_note_cost:
                    best_note_cost = note_cost
                    best_str = s_idx
                    best_fret = fret

            if best_str >= 0:
                used_strings[best_str] = True
                temp_placements[midi] = (best_str, best_fret)
                cost += best_note_cost
            else:
                cost += 500.0

        fretted_vals = [f for (_, f) in temp_placements.values() if f > 0]
        if len(fretted_vals) > 1:
            span = max(fretted_vals) - min(fretted_vals)
            if span > 4:
                cost += (span - 4) * 2000.0 + 1000.0
            elif span == 4:
                cost += 28.0

        if cost < best_cost:
            best_cost = cost
            min_fretted = min(fretted_vals) if fretted_vals else box_start
            has_barre = (fretted_vals.count(min_fretted) >= 2 and all(f > 0 for (_, f) in temp_placements.values()))

            final_map = {}
            fretted_entries = [(m, s, f) for m, (s, f) in temp_placements.items() if f > 0]
            fretted_entries.sort(key=lambda x: (x[2], -x[1]))

            finger_assignment = {}
            if has_barre:
                next_finger = 2
                for m, s, f in fretted_entries:
                    if f == min_fretted:
                        finger_assignment[m] = 1
                    else:
                        finger_assignment[m] = min(4, max(2, next_finger))
                        next_finger += 1
            elif len(fretted_entries) == 1:
                m, s, f = fretted_entries[0]
                finger_assignment[m] = min(4, max(1, 1 + (f - box_start)))
            else:
                used_fingers = [False] * 5
                for m, s, f in fretted_entries:
                    ideal = min(4, max(1, 1 + (f - min_fretted)))
                    while ideal < 4 and used_fingers[ideal]:
                        ideal += 1
                    used_fingers[ideal] = True
                    finger_assignment[m] = ideal

                for trans_midi, (s_idx, fret) in temp_placements.items():
                        finger = 0 if fret <= 0 else finger_assignment.get(trans_midi, 1)
                        final_map[trans_midi] = {"string": s_idx, "fret": fret, "finger": finger}

                        best_map = final_map
                    
                    # STICKY BOX LOGIC: If the newly fretted notes fit within the old hand posture, 
                    # keep the hand perfectly still to prevent visual jitter during arpeggios.
                if fretted_vals:
                    min_f = min(fretted_vals)
                    max_f = max(fretted_vals)
                    prev_box_min = last_box_center - 1.5
                    prev_box_max = last_box_center + 1.5
                    
                    if min_f >= prev_box_min and max_f <= prev_box_max:
                        best_box_center = last_box_center
                    else:
                        best_box_center = box_center # Use the exact center of the winning Viterbi window
                    return best_map, best_box_center

# ==============================================================================
# 3. TEST SUITE
# ==============================================================================

def run_tests():
    print("=" * 70)
    print("RUNNING BIOMECHANICAL & FORMATS VALIDATION SUITE")
    print("=" * 70)

    issues = []

    # --------------------------------------------------------------------------
    # Test A: Piano Scales & Hanon
    # --------------------------------------------------------------------------
    print("\n[1/4] Testing Piano Scales & Exercises...")
    # C Major RH Ascending (C4 to C5): Standard 1 2 3 1 2 3 4 5
    c_maj_rh = [60, 62, 64, 65, 67, 69, 71, 72]
    expected_c_maj_rh = [1, 2, 3, 1, 2, 3, 4, 5]
    res_c_maj_rh = simulate_piano_sequence(c_maj_rh, is_rh=True, initial_palm=60, initial_finger=1)
    print(f"  C Major RH Ascending: got {res_c_maj_rh}, expected {expected_c_maj_rh}")
    if res_c_maj_rh != expected_c_maj_rh:
        issues.append(f"Piano C Major RH Ascending mismatch: got {res_c_maj_rh}")

    # C Major RH Descending (C5 to C4): Standard 5 4 3 2 1 3 2 1
    c_maj_rh_desc = list(reversed(c_maj_rh))
    expected_c_maj_rh_desc = [5, 4, 3, 2, 1, 3, 2, 1]
    res_c_maj_rh_desc = simulate_piano_sequence(c_maj_rh_desc, is_rh=True, initial_palm=72, initial_finger=5)
    print(f"  C Major RH Descending: got {res_c_maj_rh_desc}, expected {expected_c_maj_rh_desc}")
    if res_c_maj_rh_desc != expected_c_maj_rh_desc:
        issues.append(f"Piano C Major RH Descending mismatch: got {res_c_maj_rh_desc}")

    # C Major LH Ascending (C3 to C4): Standard 5 4 3 2 1 3 2 1
    c_maj_lh = [48, 50, 52, 53, 55, 57, 59, 60]
    expected_c_maj_lh = [5, 4, 3, 2, 1, 3, 2, 1]
    res_c_maj_lh = simulate_piano_sequence(c_maj_lh, is_rh=False, initial_palm=48, initial_finger=5)
    print(f"  C Major LH Ascending: got {res_c_maj_lh}, expected {expected_c_maj_lh}")
    if res_c_maj_lh != expected_c_maj_lh:
        issues.append(f"Piano C Major LH Ascending mismatch: got {res_c_maj_lh}")

    # C Major LH Descending (C4 to C3): Standard 1 2 3 1 2 3 4 5
    c_maj_lh_desc = list(reversed(c_maj_lh))
    expected_c_maj_lh_desc = [1, 2, 3, 1, 2, 3, 4, 5]
    res_c_maj_lh_desc = simulate_piano_sequence(c_maj_lh_desc, is_rh=False, initial_palm=60, initial_finger=1)
    print(f"  C Major LH Descending: got {res_c_maj_lh_desc}, expected {expected_c_maj_lh_desc}")
    if res_c_maj_lh_desc != expected_c_maj_lh_desc:
        issues.append(f"Piano C Major LH Descending mismatch: got {res_c_maj_lh_desc}")

    # --------------------------------------------------------------------------
    # Test B: Guitar Chords & Positions
    # --------------------------------------------------------------------------
    print("\n[2/4] Testing Guitar Chords & Positions...")
    std_tuning = [64, 59, 55, 50, 45, 40]

    # Open C Major (48, 52, 55, 60, 64) -> C3, E3, G3, C4, E4
    c_chord = [48, 52, 55, 60, 64]
    c_map, c_center = solve_fretted_slice(c_chord, std_tuning, last_box_center=2.0)
    print(f"  Open C Major chord placement:")
    for m in c_chord:
        pl = c_map.get(m, {})
        print(f"    MIDI {m}: string {pl.get('string')} fret {pl.get('fret')} finger {pl.get('finger')}")

    c_strings = [c_map[m]['string'] for m in c_chord if m in c_map]
    if len(c_strings) != len(set(c_strings)):
        issues.append(f"Open C Major has duplicate string assignments: {c_strings}")

    # F Major Barre Chord (41, 48, 53, 58, 60, 65)
    f_barre = [41, 48, 53, 58, 60, 65]
    f_map, f_center = solve_fretted_slice(f_barre, std_tuning, last_box_center=1.5)
    print(f"  F Major Barre chord placement (frets: {[f_map[m]['fret'] for m in f_barre]}):")
    for m in f_barre:
        pl = f_map.get(m, {})
        print(f"    MIDI {m}: string {pl.get('string')} fret {pl.get('fret')} finger {pl.get('finger')}")

    f_strings = [f_map[m]['string'] for m in f_barre if m in f_map]
    if len(f_strings) != len(set(f_strings)):
        issues.append(f"F Major Barre has duplicate string assignments: {f_strings}")
    f_frets = [f_map[m]['fret'] for m in f_barre if m in f_map]
    if max(f_frets) - min(f_frets) > 3:
        issues.append(f"F Major Barre fret span exceeds 3 frets: {f_frets}")

    # Drop D low note handling
    drop_d_tuning = [64, 59, 55, 50, 45, 38]
    drop_d_chord = [38, 50, 57, 62]
    d_map, _ = solve_fretted_slice(drop_d_chord, drop_d_tuning, last_box_center=2.0)
    print(f"  Drop D chord placement (Low D 38 -> string {d_map.get(38, {}).get('string')} fret {d_map.get(38, {}).get('fret')}):")
    if d_map.get(38, {}).get('string') != 5 or d_map.get(38, {}).get('fret') != 0:
        issues.append(f"Drop D Low D2 (38) not placed on string 5 fret 0: {d_map.get(38)}")

    # --------------------------------------------------------------------------
    # Test C: External Format Inspection (MIDI, MusicXML, MXL, GP)
    # --------------------------------------------------------------------------
    print("\n[3/4] Testing MIDI and Asset File Parsing...")
    hanon_dir = "composeApp/src/androidMain/assets/midi/hanon_the_virtuoso_pianist"
    if os.path.isdir(hanon_dir):
        files = [f for f in os.listdir(hanon_dir) if f.endswith(".mid")][:5]
        for f in files:
            path = os.path.join(hanon_dir, f)
            with open(path, "rb") as mf:
                header = mf.read(14)
                if header[:4] == b"MThd":
                    fmt, tracks, division = struct.unpack(">HHH", header[8:14])
                    print(f"  Hanon MIDI file {f}: format={fmt}, tracks={tracks}, division={division} [OK]")
                else:
                    issues.append(f"Corrupted MIDI header in {f}")

    print("\n[4/4] Testing MusicXML & MXL ZIP Inspection...")
    mxl_path = "/Users/manpreet/Downloads/Clair_de_Lune__Debussy.mxl"
    if os.path.exists(mxl_path):
        try:
            with zipfile.ZipFile(mxl_path, 'r') as zf:
                xml_files = [n for n in zf.namelist() if n.endswith(".xml") and not n.startswith("META-INF")]
                if xml_files:
                    content = zf.read(xml_files[0]).decode('utf-8', errors='ignore')
                    root = ET.fromstring(content)
                    print(f"  MXL {os.path.basename(mxl_path)} contains valid MusicXML: root=<{root.tag}> [OK]")
                else:
                    issues.append(f"MXL missing xml payload in {mxl_path}")
        except Exception as e:
            issues.append(f"Failed to inspect MXL: {e}")

    # --------------------------------------------------------------------------
    # Summary
    # --------------------------------------------------------------------------
    print("\n" + "=" * 70)
    if issues:
        print(f"VALIDATION COMPLETED WITH {len(issues)} ISSUES DETECTED:")
        for idx, iss in enumerate(issues, 1):
            print(f"  {idx}. {iss}")
    else:
        print("ALL TESTS PASSED WITH 0 ISSUES! (100% SUCCESS)")
    print("=" * 70)

if __name__ == "__main__":
    run_tests()
