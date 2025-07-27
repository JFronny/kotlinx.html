import json

def extract_entities(input_file, output_file):
    with open('entities.json', 'r') as f:
        entities = json.load(f)

    entity_names = [name.lstrip('\&').rstrip(';') for name in entities.keys()]
    with open('entities.txt', 'w') as out_file:
        for i, name in enumerate(entity_names):
            if i > 0:
                out_file.write('\n')
            out_file.write(name)


if __name__ == "__main__":
    extract_entities('entities.json', 'entities.txt')
