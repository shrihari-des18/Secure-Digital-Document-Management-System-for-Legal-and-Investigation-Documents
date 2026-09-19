import cv2
import numpy as np

class TamperService:
    def __init__(self):
        # Threshold for ELA difference average to flag as tampered
        # This is a heuristic value; in production, you'd calibrate this.
        self.ela_threshold = 5.5

    def analyze_image(self, file_bytes: bytes) -> dict:
        """
        Performs Error Level Analysis (ELA) to detect digital manipulation.
        ELA resaves the image at a known quality and compares it to the original.
        Manipulated areas will have a different compression artifact level.
        """
        try:
            # Decode image from bytes
            np_arr = np.frombuffer(file_bytes, np.uint8)
            img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
            
            if img is None:
                return {"is_tampered": False, "tamper_score": 0.0}

            # ELA parameters
            quality = 90
            
            # Compress the image in memory
            encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), quality]
            _, compressed_img_bytes = cv2.imencode('.jpg', img, encode_param)
            
            # Decompress it back to an image
            compressed_img = cv2.imdecode(compressed_img_bytes, cv2.IMREAD_COLOR)
            
            # Calculate the absolute difference between the original and the compressed image
            # This highlights the compression error levels
            diff = cv2.absdiff(img, compressed_img)
            
            # Convert to grayscale for simpler analysis
            gray_diff = cv2.cvtColor(diff, cv2.COLOR_BGR2GRAY)
            
            # Enhance the differences (scale up)
            # Find the max difference in the image, scale it to 255
            max_diff = np.max(gray_diff)
            if max_diff > 0:
                scale = 255.0 / max_diff
                gray_diff = cv2.convertScaleAbs(gray_diff, alpha=scale)
            
            # Calculate the average difference across the image
            # Images that have been heavily edited or spliced often have localized spikes
            # resulting in a higher overall average or standard deviation.
            average_diff = float(np.mean(gray_diff))
            
            # Determine if it exceeds our heuristic threshold
            is_tampered = average_diff > self.ela_threshold
            
            return {
                "is_tampered": is_tampered,
                "tamper_score": round(average_diff, 2)
            }
            
        except Exception as e:
            print(f"Error during Tamper Detection: {e}")
            return {"is_tampered": False, "tamper_score": 0.0}

    def check_exif(self, file_bytes: bytes) -> dict:
        """
        Inspects EXIF metadata for anomalies, such as signatures from Photoshop or GIMP.
        """
        from PIL import Image, ExifTags
        import io
        
        try:
            img = Image.open(io.BytesIO(file_bytes))
            exif = img.getexif()
            if not exif:
                return {"exif_suspicious": False, "software": None}
            
            suspicious_keywords = ["photoshop", "gimp", "lightroom", "adobe", "canva"]
            is_suspicious = False
            detected_software = None
            
            for k, v in exif.items():
                tag = ExifTags.TAGS.get(k, k)
                if tag == 'Software':
                    software = str(v).lower()
                    detected_software = software
                    for kw in suspicious_keywords:
                        if kw in software:
                            is_suspicious = True
                            break
                            
            return {"exif_suspicious": is_suspicious, "software": detected_software}
        except Exception as e:
            print(f"Error during EXIF check: {e}")
            return {"exif_suspicious": False, "software": None}

tamper_service = TamperService()
