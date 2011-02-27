#include <jni.h>
#include <stdio.h>
#include <string.h>

JNIEXPORT jbyteArray JNICALL Java_com_yuniclient_memory_parseHexFile(JNIEnv * env, jobject this, jbyteArray fileAr, jint memsize, jint fileLenght)
{
    jbyteArray m_buffer = (*env)->NewByteArray(env, memsize);
    jbyte rec_nums[50];
    jbyte rec_nums_itr = 0;
     // + size
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
                    return NULL;
               /* if(lineLenght >= 50)
                {
                    char tmp[] = line;
                    line = new char[100];
                    for(byte z = 0; z < 50; ++z)
                        line[z] = tmp[z];
                }
                else if(lineLenght >= 100)
                    return "Wrong line length";*/
             }
        }
        if (line[0] != ':' || lineLenght % 2 != 1)
             return NULL;
        rec_nums_itr = 0;
        short i = 1;
        char *nums = (char*) malloc(2);
        while(i + 1 < lineLenght)
        {
            nums[0] = line[i];
            nums[1] = line[i+1];
            rec_nums[rec_nums_itr] = (jbyte)strtol(nums, NULL, 16);
            ++rec_nums_itr;
            i+=2;
        }
        free(nums);
        length = (short) (0xFF & (int)rec_nums[0]);
        address = (short) ((0xFF & (int)rec_nums[1]) * 0x100 + (0xFF & (int)rec_nums[2]));
        rectype = (short)(0xFF & (int)rec_nums[3]);
        if (length != rec_nums_itr - 5)
            return NULL;
        if (rectype == 2)
        {
            if (length != 2)
                return NULL;
            base_i = (short)((((0xFF & (int)rec_nums[4]) * 0x100 + (0xFF & (int)rec_nums[5])) * 16));
            continue;
        }

        if (rectype == 1)
            break;
        if (rectype != 0)
            return NULL;
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
            /*byte *result = (char *)malloc(1);
            (*env)->GetByteArrayRegion(env, m_buffer, 0, 1, result);
            if (result[0] != (jbyte)0xff)
                return NULL;
            free(result);*/

            jbyte m = (jbyte) rec_nums[i + 4];
            (*env)->SetByteArrayRegion(env, m_buffer, (base_i + address + i), 1, &m );
        }
    }
    free(file);

    return m_buffer;
}
