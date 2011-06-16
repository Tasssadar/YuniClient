#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

enum errors
{
    ERROR_WRONG_LINE_FORMAT     = 1,
    ERROR_INVALID_RECORD_LENGHT = 2,
    ERROR_INVALID_2_RECORD      = 3,
    ERROR_INVALID_RECORD_TYPE   = 4,
    ERROR_MEMORY_LOCATION       = 5
};

JNIEXPORT jbyteArray JNICALL Java_com_yuniclient_memory_parseHexFile(JNIEnv * env, jobject this, jbyteArray fileAr, jint memsize, jint fileLenght)
{
    jbyteArray m_buffer = (*env)->NewByteArray(env, memsize);
    jbyteArray m_error = (*env)->NewByteArray(env, 2);
    jbyte tmp = (jbyte) 0;
    (*env)->SetByteArrayRegion(env, m_error, 1, 1, &tmp );

    jbyte rec_nums[50];
    jbyte rec_nums_itr = 0;

    jint size = 0;
    int pos = 0;
    jbyte *file = (jbyte *)malloc(fileLenght);
    (*env)->GetByteArrayRegion(env, fileAr, 0, fileLenght, file);

    int lastSendPos = 0;
    short length = 0;
    short address = 0;
    short rectype = 0;
    short base_i = 0;
    char line[100];
    jbyte lineLenght;

    char *mem_loc_check = (char *)malloc(1);
    char *nums = (char*) malloc(2);
    short i;

    while(1)
    {
        if (pos >= fileLenght)
            break;

        line[0] = (char)file[pos];
        lineLenght = 1;
        ++pos;
        if (line[0] == ':')
        {
            while (pos + 1 != fileLenght)
            {
                if ((char)file[pos] == '\r' && (char)file[pos+1] == '\n')
                {
                    pos+=2;
                    break;
                }
                line[lineLenght] = (char)file[pos];
                ++pos;
                ++lineLenght;
                if(lineLenght >= 100)
                {
                    tmp = (jbyte) ERROR_WRONG_LINE_FORMAT;
                    (*env)->SetByteArrayRegion(env, m_error, 0, 1, &tmp );
                    return m_error;
                }
             }
        }
        if (line[0] != ':' || lineLenght % 2 != 1)
        {
               tmp = (jbyte) ERROR_WRONG_LINE_FORMAT;
               (*env)->SetByteArrayRegion(env, m_error, 0, 1, &tmp );
              return m_error;
        }
        rec_nums_itr = 0;
        i = 1;
        while(i + 1 < lineLenght)
        {
            nums[0] = line[i++];
            nums[1] = line[i++];
            rec_nums[rec_nums_itr] = (jbyte)strtol(nums, NULL, 16);
            ++rec_nums_itr;
        }

        length = (short) (0xFF & (int)rec_nums[0]);
        address = (short) ((0xFF & (int)rec_nums[1]) * 0x100 + (0xFF & (int)rec_nums[2]));
        rectype = (short)(0xFF & (int)rec_nums[3]);
        if (length != rec_nums_itr - 5)
        {
              tmp = (jbyte) ERROR_INVALID_RECORD_LENGHT;
               (*env)->SetByteArrayRegion(env, m_error, 0, 1, &tmp );
              return m_error;
        }

        if (rectype == 2)
        {
            if (length != 2)
            {
                tmp = (jbyte) ERROR_INVALID_2_RECORD;
                (*env)->SetByteArrayRegion(env, m_error, 0, 1, &tmp );
                  return m_error;
            }
            base_i = (short)((((0xFF & (int)rec_nums[4]) * 0x100 + (0xFF & (int)rec_nums[5])) * 16));
            continue;
        }

        if (rectype == 1)
            break;
        if (rectype != 0)
        {
             tmp = (jbyte) ERROR_INVALID_RECORD_TYPE;
              (*env)->SetByteArrayRegion(env, m_error, 0, 1, &tmp );
              return m_error;
        }
        i = 0;
        for (; i < length; ++i)
        {
            for (;base_i + address + i >= size; ++size)
            {
                if(size >= memsize)
                    return NULL;
                jbyte n = (jbyte) 0xFF;
                (*env)->SetByteArrayRegion(env, m_buffer, size,1, &n );
            }

            (*env)->GetByteArrayRegion(env, m_buffer, (base_i + address + i), 1, (jbyte *)mem_loc_check);
            if (mem_loc_check[0] != (char)0xff)
            {
                  tmp = (jbyte) ERROR_MEMORY_LOCATION;
                   (*env)->SetByteArrayRegion(env, m_error, 0, 1, &tmp );
                  return m_error;
            }

            jbyte m = (jbyte) rec_nums[i + 4];
            (*env)->SetByteArrayRegion(env, m_buffer, (base_i + address + i), 1, &m );
        }
    }
    free(mem_loc_check);
    free(nums);
    free(file);

    return m_buffer;
}

